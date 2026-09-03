package com.recoveriq.service.policy;

import org.springframework.stereotype.Component;

import com.recoveriq.domain.Policy;
import com.recoveriq.repository.PolicyRepository;

import lombok.RequiredArgsConstructor;

/**
 * The single point of contact with the policies table. Kept deliberately
 * thin — PolicyEngine never touches the repository directly, which is what
 * lets it be tested with plain constructed objects instead of a DB.
 */
@Component
@RequiredArgsConstructor
public class GuardrailRules {

    private final PolicyRepository policyRepository;

    public Policy getActivePolicy() {
        return policyRepository.findFirstByActiveTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "No active policy configured — check the policies table."));
    }
}