package com.recoveriq.service.ai;

import org.springframework.stereotype.Component;

import com.recoveriq.dto.AiRawRecommendation;
import com.recoveriq.dto.AiRecommendationOutput;
import com.recoveriq.enums.RecommendedAction;
import com.recoveriq.enums.RiskLevel;
import com.recoveriq.exception.InvalidAiOutputException;

@Component
public class AiOutputSchemaValidator {

    public AiRecommendationOutput validate(AiRawRecommendation raw) {
        if (raw == null) {
            throw new InvalidAiOutputException("AI output was null");
        }

        RiskLevel riskLevel = parseEnum(RiskLevel.class, raw.getRiskLevel(), "risk_level");
        RecommendedAction action = parseEnum(RecommendedAction.class, raw.getRecommendedAction(), "recommended_action");

        double recoveryProbability = requireInRange(raw.getRecoveryProbability(), "recovery_probability");
        double confidence = requireInRange(raw.getConfidence(), "confidence");

        if (raw.getReason() == null || raw.getReason().isBlank()) {
            throw new InvalidAiOutputException("AI output field 'reason' must not be blank");
        }

        return AiRecommendationOutput.builder()
                .riskLevel(riskLevel)
                .rootCause(raw.getRootCause())
                .recoveryProbability(recoveryProbability)
                .recommendedAction(action)
                .reason(raw.getReason())
                .confidence(confidence)
                .build();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String rawValue, String fieldName) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new InvalidAiOutputException("AI output field '" + fieldName + "' was missing");
        }
        try {
            return Enum.valueOf(enumClass, rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidAiOutputException(
                    "AI output field '" + fieldName + "' had unsupported value: " + rawValue);
        }
    }

    private double requireInRange(Double value, String fieldName) {
        if (value == null) {
            throw new InvalidAiOutputException("AI output field '" + fieldName + "' was missing");
        }
        if (value < 0.0 || value > 1.0) {
            throw new InvalidAiOutputException(
                    "AI output field '" + fieldName + "' out of range [0,1]: " + value);
        }
        return value;
    }
}