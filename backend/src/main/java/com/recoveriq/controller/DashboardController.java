package com.recoveriq.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recoveriq.domain.RecoveryCase;
import com.recoveriq.dto.response.DashboardSummaryResponse;
import com.recoveriq.service.scoring.ExpectedRecoveryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final ExpectedRecoveryService expectedRecoveryService;

    @GetMapping("/api/dashboard/summary")
    public DashboardSummaryResponse summary() {
        List<RecoveryCase> cases = expectedRecoveryService.scoreAllOpenCases();

        BigDecimal totalAtRisk = cases.stream()
                .map(RecoveryCase::getAmountAtRisk)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpected = cases.stream()
                .map(RecoveryCase::getExpectedRecovery)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardSummaryResponse.builder()
                .totalOpenCases(cases.size())
                .totalAmountAtRisk(totalAtRisk)
                .totalExpectedRecovery(totalExpected)
                .build();
    }
}