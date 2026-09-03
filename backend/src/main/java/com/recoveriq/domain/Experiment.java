 package com.recoveriq.domain;

import java.math.BigDecimal;
import java.time.Instant;
import com.recoveriq.enums.ExperimentStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "experiments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Experiment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExperimentStatus status;

    @Column(name = "total_at_risk_revenue", precision = 14, scale = 2)
    private BigDecimal totalAtRiskRevenue;

    @Column(name = "total_expected_recovery", precision = 14, scale = 2)
    private BigDecimal totalExpectedRecovery;

    @Column(name = "total_actual_recovered", precision = 14, scale = 2)
    private BigDecimal totalActualRecovered;

    @Column(name = "treatment_recovery_rate")
    private Double treatmentRecoveryRate;

    @Column(name = "control_recovery_rate")
    private Double controlRecoveryRate;

    @Column(name = "incremental_lift")
    private Double incrementalLift;

    @Column(name = "successful_interventions")
    private Integer successfulInterventions;

    @Column(name = "failed_interventions")
    private Integer failedInterventions;

    @Column(name = "intervention_count")
    private Integer interventionCount;

    @Column(name = "cost_per_recovery", precision = 12, scale = 2)
    private BigDecimal costPerRecovery;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.status == null) {
            this.status = ExperimentStatus.PENDING;
        }
    }
}