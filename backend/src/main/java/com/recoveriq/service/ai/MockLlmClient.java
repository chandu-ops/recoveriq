package com.recoveriq.service.ai;

import org.springframework.stereotype.Service;

import com.recoveriq.domain.RecoveryCase;
import com.recoveriq.dto.AiRawRecommendation;
import com.recoveriq.enums.FailureCategory;

/**
 * Deterministic stand-in for a real LLM call. Mirrors the reasoning a real
 * model would plausibly produce, using the same signals (failure category,
 * retry count, payment history) already used in scoring — so the mock's
 * output is at least internally consistent with Phase 3, not arbitrary.
 *
 * Swap-out plan: implement LlmClient with a real provider (e.g. OpenAiClient),
 * annotate it @Primary or remove @Service here, and nothing else in the
 * pipeline changes.
 */
@Service
public class MockLlmClient implements LlmClient {

    @Override
    public AiRawRecommendation getRecommendation(RecoveryCase rc, long successfulPaymentCount) {
        String action = mapActionFor(rc.getFailureCategory(), rc.getRetriesUsed());
        String rootCause = mapRootCauseFor(rc.getFailureCategory());
        double confidence = deriveConfidence(rc.getRecoveryProbability(), successfulPaymentCount);

        String reason = String.format(
                "Failure category %s with %d prior retr%s and %d historical successful payment%s "
                        + "is consistent with a %s outcome.",
                rc.getFailureCategory(), rc.getRetriesUsed(), rc.getRetriesUsed() == 1 ? "y" : "ies",
                successfulPaymentCount, successfulPaymentCount == 1 ? "" : "s", rootCause);

        return AiRawRecommendation.builder()
                .riskLevel(rc.getRiskLevel() != null ? rc.getRiskLevel().name() : "MEDIUM")
                .rootCause(rootCause)
                .recoveryProbability(rc.getRecoveryProbability())
                .recommendedAction(action)
                .reason(reason)
                .confidence(confidence)
                .build();
    }

    private String mapActionFor(FailureCategory category, int retriesUsed) {
        return switch (category) {
            case NETWORK_FAILURE -> retriesUsed < 2 ? "RETRY_PAYMENT" : "SEND_PAYMENT_LINK";
            case AUTHENTICATION_FAILURE -> "SEND_PAYMENT_LINK";
            case INSUFFICIENT_FUNDS -> "SEND_WHATSAPP";
            case BANK_DECLINED -> "SEND_PAYMENT_LINK";
            case CARD_EXPIRED -> "SEND_EMAIL";
            case MANDATE_CANCELLED -> "ESCALATE_HUMAN";
            case UNKNOWN -> "ESCALATE_HUMAN";
        };
    }

    private String mapRootCauseFor(FailureCategory category) {
        return switch (category) {
            case NETWORK_FAILURE -> "temporary_payment_failure";
            case AUTHENTICATION_FAILURE -> "authentication_challenge_failure";
            case INSUFFICIENT_FUNDS -> "insufficient_balance";
            case BANK_DECLINED -> "issuer_decline";
            case CARD_EXPIRED -> "expired_card";
            case MANDATE_CANCELLED -> "mandate_revoked";
            case UNKNOWN -> "unclassified_failure";
        };
    }

    private double deriveConfidence(Double recoveryProbability, long successfulPaymentCount) {
        double base = recoveryProbability != null ? recoveryProbability : 0.5;
        double historyBoost = successfulPaymentCount >= 5 ? 0.05 : 0.0;
        return Math.min(0.98, Math.round((base + historyBoost) * 100.0) / 100.0);
    }
}