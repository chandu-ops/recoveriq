package com.recoveriq.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recoveriq.domain.RecoveryCase;
import com.recoveriq.dto.response.AiRecommendationResponse;
import com.recoveriq.dto.response.RecoveryActionResponse;
import com.recoveriq.dto.response.RecoveryCaseResponse;
import com.recoveriq.service.ai.AnalyzeResult;
import com.recoveriq.service.ai.LlmRecommendationService;
import com.recoveriq.service.audit.AuditLogService;
import com.recoveriq.service.execution.ActionExecutorService;
import com.recoveriq.service.scoring.ExpectedRecoveryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RecoveryCaseController {

    private final ExpectedRecoveryService expectedRecoveryService;

    private final LlmRecommendationService llmRecommendationService;

    private final ActionExecutorService actionExecutorService;

    private final AuditLogService auditLogService;

    @GetMapping("/api/recovery/cases")
    public List<RecoveryCaseResponse> listCases() {

        List<RecoveryCase> ranked = expectedRecoveryService.scoreAllOpenCases();

        return ranked.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @PostMapping("/api/recovery/cases/{id}/analyze")
    public AiRecommendationResponse analyze(@PathVariable Long id) {

        AnalyzeResult result = llmRecommendationService.analyze(id);

        return AiRecommendationResponse.builder()
                .caseId(id)
                .riskLevel(result.aiOutput().getRiskLevel())
                .rootCause(result.aiOutput().getRootCause())
                .recoveryProbability(result.aiOutput().getRecoveryProbability())
                .recommendedAction(result.aiOutput().getRecommendedAction())
                .reason(result.aiOutput().getReason())
                .confidence(result.aiOutput().getConfidence())
                .policyDecision(result.policyResult().getDecision())
                .policyReason(result.policyResult().getReason())
                .build();
    }

    @PostMapping("/api/recovery/cases/{id}/execute")
    public RecoveryActionResponse executeCase(@PathVariable Long id) {

        var action = actionExecutorService.execute(id);

        return RecoveryActionResponse.builder()
                .caseId(id)
                .actionType(action.getActionType())
                .policyDecision(action.getPolicyDecision())
                .executionResult(action.getExecutionResult())
                .idempotencyKey(action.getIdempotencyKey())
                .executedAt(action.getExecutedAt())
                .build();
    }

    @PostMapping("/api/recovery/cases/{id}/stop")
    public void stopCase(@PathVariable Long id) {

        RecoveryCase rc = expectedRecoveryService.scoreAllOpenCases().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No case " + id));

        rc.setStatus(com.recoveriq.enums.CaseStatus.STOPPED);

        auditLogService.log(rc, "MANUALLY_STOPPED", "Stopped via API");
    }

    private RecoveryCaseResponse toResponse(RecoveryCase rc) {

        return RecoveryCaseResponse.builder()
                .id(rc.getId())
                .customerName(rc.getCustomer().getName())
                .amountAtRisk(rc.getAmountAtRisk())
                .failureCategory(rc.getFailureCategory())
                .riskLevel(rc.getRiskLevel())
                .recoveryProbability(rc.getRecoveryProbability())
                .expectedRecovery(rc.getExpectedRecovery())
                .status(rc.getStatus())
                .build();
    }
}