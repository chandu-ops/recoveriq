package com.recoveriq.domain;

import java.math.BigDecimal;
import java.time.Instant;
import com.recoveriq.enums.*;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recovery_cases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecoveryCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name = "amount_at_risk", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountAtRisk;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_category", nullable = false, length = 40)
    private FailureCategory failureCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 10)
    private RiskLevel riskLevel;

    @Column(name = "recovery_probability")
    private Double recoveryProbability;

    @Column(name = "expected_recovery", precision = 12, scale = 2)
    private BigDecimal expectedRecovery;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_action", length = 30)
    private RecommendedAction recommendedAction;

    @Column(name = "ai_reasoning", columnDefinition = "TEXT")
    private String aiReasoning;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "experiment_group", length = 10)
    private ExperimentGroup experimentGroup;

    @Column(name = "retries_used", nullable = false)
    private int retriesUsed;

    @Column(name = "contacts_used", nullable = false)
    private int contactsUsed;

    @Column(name = "messages_used", nullable = false)
    private int messagesUsed;

    @Column(name = "human_escalations_used", nullable = false)
    private int humanEscalationsUsed;

    @Column(name = "recovery_window_deadline")
    private Instant recoveryWindowDeadline;

    @Column(name = "actual_recovered_amount", precision = 12, scale = 2)
    private BigDecimal actualRecoveredAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = CaseStatus.OPEN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}