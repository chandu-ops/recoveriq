package com.recoveriq.dto;

import com.recoveriq.enums.PolicyDecision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PolicyEvaluationResult {
    private PolicyDecision decision;
    private String reason;
}