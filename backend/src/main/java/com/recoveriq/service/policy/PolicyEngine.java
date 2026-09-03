package com.recoveriq.service.policy;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.recoveriq.domain.Policy;
import com.recoveriq.domain.RecoveryCase;
import com.recoveriq.dto.AiRecommendationOutput;
import com.recoveriq.dto.PolicyEvaluationResult;
import com.recoveriq.enums.PaymentStatus;
import com.recoveriq.enums.PolicyDecision;
import com.recoveriq.enums.RecommendedAction;

/**
 * The deterministic authority. No Spring-managed dependencies beyond the
 * annotation — every input arrives as a plain parameter, which is what
 * keeps this class fully unit-testable without a database.
 *
 * AI recommendation != authorization. This class is the authorization.
 */
@Component
public class PolicyEngine {

    // Not part of the merchant-configurable `policies` table by design —
    // this is a model-confidence threshold, not a financial/contact limit.
    // Tune here if 0.50 proves too strict/loose during testing.
    private static final double CONFIDENCE_THRESHOLD = 0.50;

    private static final Set<RecommendedAction> CONTACT_ACTIONS = EnumSet.of(
            RecommendedAction.SEND_PAYMENT_LINK,
            RecommendedAction.SEND_WHATSAPP,
            RecommendedAction.SEND_EMAIL
    );

    public PolicyEvaluationResult evaluate(RecoveryCase rc, AiRecommendationOutput ai, Policy policy) {

        if (rc.getPayment() != null && rc.getPayment().getStatus() == PaymentStatus.SUCCEEDED) {
            return stop("Payment already succeeded — no further action needed");
        }

        Instant recoveryDeadline = rc.getCreatedAt().plus(Duration.ofDays(policy.getMaxRecoveryWindowDays()));
        if (Instant.now().isAfter(recoveryDeadline)) {
            return stop("Recovery window of " + policy.getMaxRecoveryWindowDays() + " days has expired");
        }

        RecommendedAction action = ai.getRecommendedAction();

        if (action == RecommendedAction.RETRY_PAYMENT && rc.getRetriesUsed() >= policy.getMaxRetries()) {
            return stop("Maximum retry limit reached (" + policy.getMaxRetries() + ")");
        }

        if (CONTACT_ACTIONS.contains(action) && rc.getContactsUsed() >= policy.getMaxContacts()) {
            return stop("Maximum contact limit reached (" + policy.getMaxContacts() + ")");
        }

        if (CONTACT_ACTIONS.contains(action) && rc.getMessagesUsed() >= policy.getMaxMessages()) {
            return stop("Maximum message limit reached (" + policy.getMaxMessages() + ")");
        }

        if (action == RecommendedAction.ESCALATE_HUMAN
                && rc.getHumanEscalationsUsed() >= policy.getMaxHumanEscalations()) {
            return stop("Maximum human escalation limit reached (" + policy.getMaxHumanEscalations() + ")");
        }

        // Defensive check: AiOutputSchemaValidator (Phase 4) already guarantees `action`
        // is a valid enum member, so this branch is effectively unreachable today.
        // Kept anyway as a second, independent layer — "if AI recommends unsupported
        // action -> BLOCK" should hold even if a future validator change weakens Phase 4.
        if (!EnumSet.allOf(RecommendedAction.class).contains(action)) {
            return block("Recommended action is not in the supported action set");
        }

        if (ai.getConfidence() < CONFIDENCE_THRESHOLD) {
            return escalate("Confidence " + ai.getConfidence() + " below threshold " + CONFIDENCE_THRESHOLD);
        }

        return allow("All guardrail checks passed");
    }

    private PolicyEvaluationResult stop(String reason) {
        return PolicyEvaluationResult.builder().decision(PolicyDecision.STOP).reason(reason).build();
    }

    private PolicyEvaluationResult block(String reason) {
        return PolicyEvaluationResult.builder().decision(PolicyDecision.BLOCK).reason(reason).build();
    }

    private PolicyEvaluationResult escalate(String reason) {
        return PolicyEvaluationResult.builder().decision(PolicyDecision.ESCALATE).reason(reason).build();
    }

    private PolicyEvaluationResult allow(String reason) {
        return PolicyEvaluationResult.builder().decision(PolicyDecision.ALLOW).reason(reason).build();
    }
}