package com.recoveriq.dto.response;

import java.math.BigDecimal;

import com.recoveriq.enums.CaseStatus;
import com.recoveriq.enums.FailureCategory;
import com.recoveriq.enums.RiskLevel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RecoveryCaseResponse {
    private Long id;
    private String customerName;
    private BigDecimal amountAtRisk;
    private FailureCategory failureCategory;
    private RiskLevel riskLevel;
    private Double recoveryProbability;
    private BigDecimal expectedRecovery;
    private CaseStatus status;
}