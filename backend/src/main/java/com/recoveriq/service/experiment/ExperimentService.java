 package com.recoveriq.service.experiment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.recoveriq.domain.Experiment;
import com.recoveriq.domain.RecoveryCase;
import com.recoveriq.enums.CaseStatus;
import com.recoveriq.enums.ExperimentGroup;
import com.recoveriq.enums.ExperimentStatus;
import com.recoveriq.repository.ExperimentRepository;
import com.recoveriq.repository.RecoveryCaseRepository;
import com.recoveriq.service.data.SyntheticBatchGeneratorService;
import com.recoveriq.service.scoring.ExpectedRecoveryService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExperimentService {
    private final RecoveryCaseRepository recoveryCaseRepository;
    private final ExperimentRepository experimentRepository;
    private final ExpectedRecoveryService expectedRecoveryService;
    private final SyntheticBatchGeneratorService generatorService;

    // Static baseline: no personalization, flat reminder strategy — this is
    // the "control" a merchant would run without RecoverIQ.
    private static final double CONTROL_SUCCESS_PROBABILITY = 0.20;

    @Transactional
    public Experiment runExperiment(int batchSize) {
        Experiment experiment = experimentRepository.save(Experiment.builder()
                .name("Batch experiment " + Instant.now())
                .status(ExperimentStatus.RUNNING)
                .startedAt(Instant.now())
                .build());

        List<RecoveryCase> cases = generatorService.generateBatch(batchSize);
        for (int i = 0; i < cases.size(); i++) {
            cases.get(i).setExperimentGroup(i % 2 == 0 ? ExperimentGroup.TREATMENT : ExperimentGroup.CONTROL);
        }

        BigDecimal totalAtRisk = BigDecimal.ZERO, totalExpected = BigDecimal.ZERO,
                totalActual = BigDecimal.ZERO, totalCost = BigDecimal.ZERO;
        int treatmentTotal = 0, treatmentSuccess = 0, controlTotal = 0, controlSuccess = 0;
        int successfulInterventions = 0, failedInterventions = 0, interventionCount = 0;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (RecoveryCase rc : cases) {
            totalAtRisk = totalAtRisk.add(rc.getAmountAtRisk());
            double probability;
            BigDecimal interventionCost;

            if (rc.getExperimentGroup() == ExperimentGroup.TREATMENT) {
                long syntheticHistory = random.nextInt(0, 15); // documented synthetic signal, not a DB query
                probability = expectedRecoveryService.computeProbability(rc.getFailureCategory(), rc.getRetriesUsed(), syntheticHistory);
                interventionCost = BigDecimal.valueOf(1.5);
                treatmentTotal++;
            } else {
                probability = CONTROL_SUCCESS_PROBABILITY;
                interventionCost = BigDecimal.valueOf(1.0);
                controlTotal++;
            }

            totalExpected = totalExpected.add(expectedRecoveryService.computeExpectedRecovery(rc.getAmountAtRisk(), probability));
            interventionCount++;
            totalCost = totalCost.add(interventionCost);

            boolean success = random.nextDouble() < probability;
            if (success) {
                totalActual = totalActual.add(rc.getAmountAtRisk());
                successfulInterventions++;
                if (rc.getExperimentGroup() == ExperimentGroup.TREATMENT) treatmentSuccess++; else controlSuccess++;
                rc.setStatus(CaseStatus.RECOVERED);
                rc.setActualRecoveredAmount(rc.getAmountAtRisk());
            } else {
                failedInterventions++;
                rc.setStatus(CaseStatus.FAILED);
            }
        }
        recoveryCaseRepository.saveAll(cases);

        double treatmentRate = treatmentTotal == 0 ? 0 : (double) treatmentSuccess / treatmentTotal;
        double controlRate = controlTotal == 0 ? 0 : (double) controlSuccess / controlTotal;
        double lift = controlRate == 0 ? 0 : (treatmentRate - controlRate) / controlRate;

        experiment.setTotalAtRiskRevenue(totalAtRisk);
        experiment.setTotalExpectedRecovery(totalExpected);
        experiment.setTotalActualRecovered(totalActual);
        experiment.setTreatmentRecoveryRate(round2(treatmentRate));
        experiment.setControlRecoveryRate(round2(controlRate));
        experiment.setIncrementalLift(round2(lift));
        experiment.setSuccessfulInterventions(successfulInterventions);
        experiment.setFailedInterventions(failedInterventions);
        experiment.setInterventionCount(interventionCount);
        experiment.setCostPerRecovery(successfulInterventions == 0 ? BigDecimal.ZERO :
                totalCost.divide(BigDecimal.valueOf(successfulInterventions), 2, RoundingMode.HALF_UP));
        experiment.setStatus(ExperimentStatus.COMPLETED);
        experiment.setCompletedAt(Instant.now());

        return experimentRepository.save(experiment);
    }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}