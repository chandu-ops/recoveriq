package com.recoveriq.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DashboardSummaryResponse {
    private long totalOpenCases;
    private BigDecimal totalAmountAtRisk;
    private BigDecimal totalExpectedRecovery;
}