package com.recoveriq.dto.response;

import java.time.Instant;
import com.recoveriq.enums.ExecutionResult;
import com.recoveriq.enums.PolicyDecision;
import com.recoveriq.enums.RecommendedAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RecoveryActionResponse {
    private Long caseId;
    private RecommendedAction actionType;
    private PolicyDecision policyDecision;
    private ExecutionResult executionResult;
    private String idempotencyKey;
    private Instant executedAt;
}