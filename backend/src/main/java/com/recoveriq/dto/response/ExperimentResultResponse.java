package com.recoveriq.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExperimentResultResponse {
    private Long id;
    private String status;
    private BigDecimal totalAtRiskRevenue;
    private BigDecimal totalExpectedRecovery;
    private BigDecimal totalActualRecovered;
    private Double treatmentRecoveryRate;
    private Double controlRecoveryRate;
    private Double incrementalLift;
    private Integer successfulInterventions;
    private Integer failedInterventions;
    private Integer interventionCount;
    private BigDecimal costPerRecovery;
}