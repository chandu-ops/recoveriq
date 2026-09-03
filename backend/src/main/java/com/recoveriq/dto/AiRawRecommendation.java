package com.recoveriq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class AiRawRecommendation {
    private String riskLevel;
    private String rootCause;
    private Double recoveryProbability;
    private String recommendedAction;
    private String reason;
    private Double confidence;
}