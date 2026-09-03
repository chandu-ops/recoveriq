package com.recoveriq.dto;

import com.recoveriq.enums.RecommendedAction;
import com.recoveriq.enums.RiskLevel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Only ever constructed by AiOutputSchemaValidator after every field has
 * been checked. Nothing downstream (policy engine, controllers) should
 * ever see an AiRawRecommendation directly.
 */
@Getter
@Builder
@AllArgsConstructor
public class AiRecommendationOutput {
    private RiskLevel riskLevel;
    private String rootCause;
    private double recoveryProbability;
    private RecommendedAction recommendedAction;
    private String reason;
    private double confidence;
}