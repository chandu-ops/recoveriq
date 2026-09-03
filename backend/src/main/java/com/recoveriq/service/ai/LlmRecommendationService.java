package com.recoveriq.service.ai;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recoveriq.domain.AuditLog;
import com.recoveriq.domain.Policy;
import com.recoveriq.domain.RecoveryAction;
import com.recoveriq.domain.RecoveryCase;
import com.recoveriq.dto.AiRawRecommendation;
import com.recoveriq.dto.AiRecommendationOutput;
import com.recoveriq.dto.PolicyEvaluationResult;
import com.recoveriq.enums.CaseStatus;
import com.recoveriq.enums.PaymentStatus;
import com.recoveriq.exception.InvalidAiOutputException;
import com.recoveriq.repository.AuditLogRepository;
import com.recoveriq.repository.PaymentRepository;
import com.recoveriq.repository.RecoveryActionRepository;
import com.recoveriq.repository.RecoveryCaseRepository;
import com.recoveriq.service.policy.GuardrailRules;
import com.recoveriq.service.policy.PolicyEngine;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LlmRecommendationService {

    private final LlmClient llmClient;
    private final AiOutputSchemaValidator validator;
    private final RecoveryCaseRepository recoveryCaseRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogRepository auditLogRepository;
    private final RecoveryActionRepository recoveryActionRepository;
    private final PolicyEngine policyEngine;
    private final GuardrailRules guardrailRules;

    @Transactional
    public AnalyzeResult analyze(Long caseId) {
        RecoveryCase rc = recoveryCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("No recovery case with id " + caseId));

        long successfulPayments = paymentRepository.countByCustomerIdAndStatus(
                rc.getCustomer().getId(), PaymentStatus.SUCCEEDED);

        AiRawRecommendation raw = llmClient.getRecommendation(rc, successfulPayments);

        AiRecommendationOutput validated;
        try {
            validated = validator.validate(raw);
        } catch (InvalidAiOutputException ex) {
            rc.setStatus(CaseStatus.HUMAN_REVIEW);
            recoveryCaseRepository.save(rc);
            logAudit(rc, "AI_OUTPUT_INVALID", ex.getMessage());
            throw ex;
        }

        rc.setRecommendedAction(validated.getRecommendedAction());
        rc.setAiReasoning(validated.getReason());
        rc.setConfidenceScore(validated.getConfidence());

        Policy policy = guardrailRules.getActivePolicy();
        PolicyEvaluationResult policyResult = policyEngine.evaluate(rc, validated, policy);

        recoveryActionRepository.save(RecoveryAction.builder()
                .recoveryCase(rc)
                .actionType(validated.getRecommendedAction())
                .policyDecision(policyResult.getDecision())
                .idempotencyKey(UUID.randomUUID().toString())
                .build());

        switch (policyResult.getDecision()) {
            case STOP -> rc.setStatus(CaseStatus.STOPPED);
            case ESCALATE -> rc.setStatus(CaseStatus.HUMAN_REVIEW);
            case ALLOW -> rc.setStatus(CaseStatus.IN_PROGRESS);
            case BLOCK -> { /* action-level rejection; case status unchanged */ }
        }
        recoveryCaseRepository.save(rc);

        logAudit(rc, "POLICY_EVALUATED",
                policyResult.getDecision() + ": " + policyResult.getReason());

        return new AnalyzeResult(validated, policyResult);
    }

    private void logAudit(RecoveryCase rc, String eventType, String detail) {
        auditLogRepository.save(AuditLog.builder()
                .recoveryCase(rc)
                .eventType(eventType)
                .eventDetail(detail)
                .build());
    }
}