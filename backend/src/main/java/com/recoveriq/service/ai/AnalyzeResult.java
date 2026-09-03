package com.recoveriq.service.ai;

import com.recoveriq.dto.AiRecommendationOutput;
import com.recoveriq.dto.PolicyEvaluationResult;

/** Internal carrier between service and controller — not a REST DTO. */
public record AnalyzeResult(AiRecommendationOutput aiOutput, PolicyEvaluationResult policyResult) {
}