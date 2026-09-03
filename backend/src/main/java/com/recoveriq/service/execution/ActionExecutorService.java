package com.recoveriq.service.execution;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.recoveriq.domain.RecoveryAction;
import com.recoveriq.domain.RecoveryCase;
import com.recoveriq.enums.CaseStatus;
import com.recoveriq.enums.ExecutionResult;
import com.recoveriq.enums.PolicyDecision;
import com.recoveriq.enums.RecommendedAction;
import com.recoveriq.repository.RecoveryActionRepository;
import com.recoveriq.repository.RecoveryCaseRepository;
import com.recoveriq.service.audit.AuditLogService;
import lombok.RequiredArgsConstructor;

/**
 * Executes only ALLOW-ed actions. Simulates outcome (no real payment
 * gateway/messaging integration yet) using recoveryProbability as the
 * success chance — documented simplification, not hidden randomness.
 */
@Service
@RequiredArgsConstructor
public class ActionExecutorService {

    private final RecoveryCaseRepository recoveryCaseRepository;
    private final RecoveryActionRepository recoveryActionRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public RecoveryAction execute(Long caseId) {
        RecoveryCase rc = recoveryCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("No case " + caseId));

        RecoveryAction action = recoveryActionRepository
                .findTopByRecoveryCaseIdOrderByCreatedAtDesc(caseId)
                .orElseThrow(() -> new IllegalStateException("No action recorded — call /analyze first"));

        if (action.getPolicyDecision() != PolicyDecision.ALLOW) {
            throw new IllegalStateException("Cannot execute — last policy decision was " + action.getPolicyDecision());
        }
        if (action.getExecutionResult() != null) {
            throw new IllegalStateException("Action already executed (idempotency key " + action.getIdempotencyKey() + ")");
        }

        boolean success = Math.random() < (rc.getRecoveryProbability() != null ? rc.getRecoveryProbability() : 0.5);

        incrementCounter(rc, action.getActionType());

        action.setExecutionResult(success ? ExecutionResult.SUCCESS : ExecutionResult.FAILURE);
        action.setExecutedAt(Instant.now());
        recoveryActionRepository.save(action);

        if (success) {
            rc.setStatus(CaseStatus.RECOVERED);
            rc.setActualRecoveredAmount(rc.getAmountAtRisk());
        } else {
            rc.setStatus(CaseStatus.FAILED);
        }
        recoveryCaseRepository.save(rc);

        auditLogService.log(rc, "ACTION_EXECUTED",
                action.getActionType() + " -> " + action.getExecutionResult());

        return action;
    }

    private void incrementCounter(RecoveryCase rc, RecommendedAction action) {
        switch (action) {
            case RETRY_PAYMENT -> rc.setRetriesUsed(rc.getRetriesUsed() + 1);
            case SEND_PAYMENT_LINK, SEND_WHATSAPP, SEND_EMAIL -> {
                rc.setContactsUsed(rc.getContactsUsed() + 1);
                rc.setMessagesUsed(rc.getMessagesUsed() + 1);
            }
            case ESCALATE_HUMAN -> rc.setHumanEscalationsUsed(rc.getHumanEscalationsUsed() + 1);
            case WAIT, STOP -> { /* no counter */ }
        }
    }
}