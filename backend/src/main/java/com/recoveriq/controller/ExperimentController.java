package com.recoveriq.controller;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;
import com.recoveriq.domain.Experiment;
import com.recoveriq.dto.response.ExperimentResultResponse;
import com.recoveriq.repository.ExperimentRepository;
import com.recoveriq.service.experiment.ExperimentService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ExperimentController {
    private final ExperimentService experimentService;
    private final ExperimentRepository experimentRepository;

    @PostMapping("/api/experiments/run")
    public ExperimentResultResponse run(@RequestParam(defaultValue = "5000") int batchSize) {
        return toResponse(experimentService.runExperiment(batchSize));
    }

    @GetMapping("/api/experiments/results")
    public List<ExperimentResultResponse> results() {
        return experimentRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    private ExperimentResultResponse toResponse(Experiment e) {
        return ExperimentResultResponse.builder()
                .id(e.getId()).status(e.getStatus().name())
                .totalAtRiskRevenue(e.getTotalAtRiskRevenue())
                .totalExpectedRecovery(e.getTotalExpectedRecovery())
                .totalActualRecovered(e.getTotalActualRecovered())
                .treatmentRecoveryRate(e.getTreatmentRecoveryRate())
                .controlRecoveryRate(e.getControlRecoveryRate())
                .incrementalLift(e.getIncrementalLift())
                .successfulInterventions(e.getSuccessfulInterventions())
                .failedInterventions(e.getFailedInterventions())
                .interventionCount(e.getInterventionCount())
                .costPerRecovery(e.getCostPerRecovery())
                .build();
    }
}