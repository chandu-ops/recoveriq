package com.recoveriq.service.ai;

import com.recoveriq.domain.RecoveryCase;
import com.recoveriq.dto.AiRawRecommendation;

public interface LlmClient {
    /**
     * Returns raw, untrusted output — the caller MUST run this through
     * AiOutputSchemaValidator before acting on any field.
     */
    AiRawRecommendation getRecommendation(RecoveryCase recoveryCase, long successfulPaymentCount);
}