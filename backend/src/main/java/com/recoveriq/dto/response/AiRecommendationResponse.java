package com.recoveriq.dto.response;

import com.recoveriq.enums.PolicyDecision;
import com.recoveriq.enums.RecommendedAction;
import com.recoveriq.enums.RiskLevel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AiRecommendationResponse {
    private Long caseId;
    private RiskLevel riskLevel;
    private String rootCause;
    private double recoveryProbability;
    private RecommendedAction recommendedAction;
    private String reason;
    private double confidence;
    private PolicyDecision policyDecision;
    private String policyReason;
}