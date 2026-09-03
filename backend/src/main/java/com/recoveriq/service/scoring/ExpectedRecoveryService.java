 package com.recoveriq.service.scoring;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recoveriq.domain.RecoveryCase;
import com.recoveriq.enums.CaseStatus;
import com.recoveriq.enums.FailureCategory;
import com.recoveriq.enums.PaymentStatus;
import com.recoveriq.enums.RiskLevel;
import com.recoveriq.repository.PaymentRepository;
import com.recoveriq.repository.RecoveryCaseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpectedRecoveryService {

    private final RecoveryCaseRepository recoveryCaseRepository;
    private final PaymentRepository paymentRepository;

    private static final double MIN_PROBABILITY = 0.02;
    private static final double MAX_PROBABILITY = 0.98;
    private static final double RETRY_PENALTY_PER_ATTEMPT = 0.08;

    /**
     * Pure function — no DB access. Kept separate from scoreAllOpenCases()
     * specifically so it can be unit tested without mocking repositories.
     */
    public double computeProbability(FailureCategory category, int retriesUsed, long successfulPaymentCount) {
        double base = baseProbability(category);
        double historyAdjustment = historyAdjustment(successfulPaymentCount);
        double retryPenalty = retriesUsed * RETRY_PENALTY_PER_ATTEMPT;

        double raw = base + historyAdjustment - retryPenalty;
        double clamped = clamp(raw, MIN_PROBABILITY, MAX_PROBABILITY);

        // Round to 2 decimals — avoids floating-point display noise (e.g. 0.9500000000000001)
        // in API responses and the dashboard, with no meaningful precision loss for this heuristic.
        return Math.round(clamped * 100.0) / 100.0;
    }

    public RiskLevel deriveRiskLevel(double probability) {
        if (probability < 0.30) return RiskLevel.HIGH;
        if (probability < 0.60) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    public BigDecimal computeExpectedRecovery(BigDecimal amountAtRisk, double probability) {
        return amountAtRisk
                .multiply(BigDecimal.valueOf(probability))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Orchestration: scores every OPEN case, persists the result, returns
     * the full set ranked by expected_recovery descending.
     */
    @Transactional
    public List<RecoveryCase> scoreAllOpenCases() {
        List<RecoveryCase> openCases = recoveryCaseRepository.findByStatus(CaseStatus.OPEN);

        for (RecoveryCase rc : openCases) {
            long successfulPayments = paymentRepository.countByCustomerIdAndStatus(
                    rc.getCustomer().getId(), PaymentStatus.SUCCEEDED);

            double probability = computeProbability(
                    rc.getFailureCategory(), rc.getRetriesUsed(), successfulPayments);

            rc.setRecoveryProbability(probability);
            rc.setRiskLevel(deriveRiskLevel(probability));
            rc.setExpectedRecovery(computeExpectedRecovery(rc.getAmountAtRisk(), probability));
        }

        recoveryCaseRepository.saveAll(openCases);

        return recoveryCaseRepository.findTop50OpenOrderedByExpectedRecovery(
                org.springframework.data.domain.PageRequest.of(0, 50));    }

    private double baseProbability(FailureCategory category) {
        return switch (category) {
            case NETWORK_FAILURE -> 0.80;
            case AUTHENTICATION_FAILURE -> 0.55;
            case INSUFFICIENT_FUNDS -> 0.35;
            case BANK_DECLINED -> 0.30;
            case CARD_EXPIRED -> 0.25;
            case MANDATE_CANCELLED -> 0.10;
            case UNKNOWN -> 0.20;
        };
    }

    private double historyAdjustment(long successfulPaymentCount) {
        if (successfulPaymentCount >= 10) return 0.15;
        if (successfulPaymentCount >= 5) return 0.10;
        if (successfulPaymentCount >= 1) return 0.05;
        return 0.00;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}