package com.recoveriq.service.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.recoveriq.domain.Customer;
import com.recoveriq.domain.Payment;
import com.recoveriq.domain.Policy;
import com.recoveriq.domain.RecoveryCase;
import com.recoveriq.dto.AiRecommendationOutput;
import com.recoveriq.enums.CaseStatus;
import com.recoveriq.enums.FailureCategory;
import com.recoveriq.enums.PaymentStatus;
import com.recoveriq.enums.PolicyDecision;
import com.recoveriq.enums.RecommendedAction;
import com.recoveriq.enums.RiskLevel;

class PolicyEngineTest {

    private final PolicyEngine engine = new PolicyEngine();

    private Policy defaultPolicy() {
        return Policy.builder()
                .maxRetries(2).maxContacts(3).maxMessages(3)
                .maxHumanEscalations(1).maxRecoveryWindowDays(7)
                .active(true).build();
    }

    private RecoveryCase baseCase() {
        return RecoveryCase.builder()
                .customer(Customer.builder().name("Test").build())
                .amountAtRisk(BigDecimal.valueOf(10000))
                .failureCategory(FailureCategory.NETWORK_FAILURE)
                .status(CaseStatus.OPEN)
                .retriesUsed(0).contactsUsed(0).messagesUsed(0).humanEscalationsUsed(0)
                .createdAt(Instant.now())
                .build();
    }

    private AiRecommendationOutput aiOutput(RecommendedAction action, double confidence) {
        return AiRecommendationOutput.builder()
                .riskLevel(RiskLevel.MEDIUM)
                .rootCause("test_cause")
                .recoveryProbability(0.5)
                .recommendedAction(action)
                .reason("test reason")
                .confidence(confidence)
                .build();
    }

    @Test
    void paymentAlreadySucceeded_returnsStop() {
        RecoveryCase rc = baseCase();
        rc.setPayment(Payment.builder().status(PaymentStatus.SUCCEEDED).build());
        var result = engine.evaluate(rc, aiOutput(RecommendedAction.RETRY_PAYMENT, 0.9), defaultPolicy());
        assertEquals(PolicyDecision.STOP, result.getDecision());
    }

    @Test
    void retryLimitReached_returnsStop() {
        RecoveryCase rc = baseCase();
        rc.setRetriesUsed(2); // == maxRetries
        var result = engine.evaluate(rc, aiOutput(RecommendedAction.RETRY_PAYMENT, 0.9), defaultPolicy());
        assertEquals(PolicyDecision.STOP, result.getDecision());
    }

    @Test
    void contactLimitReached_returnsStop() {
        RecoveryCase rc = baseCase();
        rc.setContactsUsed(3); // == maxContacts
        var result = engine.evaluate(rc, aiOutput(RecommendedAction.SEND_WHATSAPP, 0.9), defaultPolicy());
        assertEquals(PolicyDecision.STOP, result.getDecision());
    }

    @Test
    void messageLimitReached_returnsStop() {
        RecoveryCase rc = baseCase();
        rc.setMessagesUsed(3); // == maxMessages
        var result = engine.evaluate(rc, aiOutput(RecommendedAction.SEND_EMAIL, 0.9), defaultPolicy());
        assertEquals(PolicyDecision.STOP, result.getDecision());
    }

    @Test
    void humanEscalationLimitReached_returnsStop() {
        RecoveryCase rc = baseCase();
        rc.setHumanEscalationsUsed(1); // == maxHumanEscalations
        var result = engine.evaluate(rc, aiOutput(RecommendedAction.ESCALATE_HUMAN, 0.9), defaultPolicy());
        assertEquals(PolicyDecision.STOP, result.getDecision());
    }

    @Test
    void recoveryWindowExpired_returnsStop() {
        RecoveryCase rc = baseCase();
        rc.setCreatedAt(Instant.now().minus(Duration.ofDays(8))); // > 7-day window
        var result = engine.evaluate(rc, aiOutput(RecommendedAction.RETRY_PAYMENT, 0.9), defaultPolicy());
        assertEquals(PolicyDecision.STOP, result.getDecision());
    }

    @Test
    void lowConfidence_returnsEscalate() {
        RecoveryCase rc = baseCase();
        var result = engine.evaluate(rc, aiOutput(RecommendedAction.RETRY_PAYMENT, 0.30), defaultPolicy());
        assertEquals(PolicyDecision.ESCALATE, result.getDecision());
    }

    @Test
    void allGuardrailsClear_returnsAllow() {
        RecoveryCase rc = baseCase();
        var result = engine.evaluate(rc, aiOutput(RecommendedAction.RETRY_PAYMENT, 0.85), defaultPolicy());
        assertEquals(PolicyDecision.ALLOW, result.getDecision());
    }
}