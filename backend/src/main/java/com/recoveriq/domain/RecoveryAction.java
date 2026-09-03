package com.recoveriq.domain;

import java.time.Instant;
import com.recoveriq.enums.ExecutionResult;
import com.recoveriq.enums.PolicyDecision;
import com.recoveriq.enums.RecommendedAction;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recovery_actions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecoveryAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recovery_case_id", nullable = false)
    private RecoveryCase recoveryCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private RecommendedAction actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_decision", nullable = false, length = 20)
    private PolicyDecision policyDecision;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_result", length = 20)
    private ExecutionResult executionResult;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}