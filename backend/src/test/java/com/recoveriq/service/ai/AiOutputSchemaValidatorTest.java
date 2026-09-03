package com.recoveriq.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.recoveriq.dto.AiRawRecommendation;
import com.recoveriq.dto.AiRecommendationOutput;
import com.recoveriq.enums.RecommendedAction;
import com.recoveriq.enums.RiskLevel;
import com.recoveriq.exception.InvalidAiOutputException;

class AiOutputSchemaValidatorTest {

    private final AiOutputSchemaValidator validator =
            new AiOutputSchemaValidator();

    private AiRawRecommendation validRaw() {

        return AiRawRecommendation.builder()
                .riskLevel("HIGH")
                .rootCause("temporary_payment_failure")
                .recoveryProbability(0.82)
                .recommendedAction("RETRY_PAYMENT")
                .reason("Consistent with a temporary failure pattern.")
                .confidence(0.89)
                .build();
    }

    @Test
    void validInputPassesAndMapsCorrectly() {

        AiRecommendationOutput result =
                validator.validate(validRaw());

        assertEquals(RiskLevel.HIGH, result.getRiskLevel());

        assertEquals(
                RecommendedAction.RETRY_PAYMENT,
                result.getRecommendedAction()
        );
    }

    @Test
    void unsupportedRiskLevelThrows() {

        AiRawRecommendation raw =
                validRaw().toBuilder()
                        .riskLevel("CATASTROPHIC")
                        .build();

        assertThrows(
                InvalidAiOutputException.class,
                () -> validator.validate(raw)
        );
    }

    @Test
    void unsupportedActionThrows() {

        AiRawRecommendation raw =
                validRaw().toBuilder()
                        .recommendedAction("REFUND_EVERYTHING")
                        .build();

        assertThrows(
                InvalidAiOutputException.class,
                () -> validator.validate(raw)
        );
    }

    @Test
    void outOfRangeConfidenceThrows() {

        AiRawRecommendation raw =
                validRaw().toBuilder()
                        .confidence(1.5)
                        .build();

        assertThrows(
                InvalidAiOutputException.class,
                () -> validator.validate(raw)
        );
    }

    @Test
    void outOfRangeProbabilityThrows() {

        AiRawRecommendation raw =
                validRaw().toBuilder()
                        .recoveryProbability(-0.1)
                        .build();

        assertThrows(
                InvalidAiOutputException.class,
                () -> validator.validate(raw)
        );
    }

    @Test
    void blankReasonThrows() {

        AiRawRecommendation raw =
                validRaw().toBuilder()
                        .reason("  ")
                        .build();

        assertThrows(
                InvalidAiOutputException.class,
                () -> validator.validate(raw)
        );
    }

    @Test
    void nullRequiredFieldThrows() {

        AiRawRecommendation raw =
                validRaw().toBuilder()
                        .recommendedAction(null)
                        .build();

        assertThrows(
                InvalidAiOutputException.class,
                () -> validator.validate(raw)
        );
    }
}