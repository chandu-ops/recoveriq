package com.recoveriq.domain;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "policies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries;

    @Column(name = "max_contacts", nullable = false)
    private int maxContacts;

    @Column(name = "max_messages", nullable = false)
    private int maxMessages;

    @Column(name = "max_human_escalations", nullable = false)
    private int maxHumanEscalations;

    @Column(name = "max_recovery_window_days", nullable = false)
    private int maxRecoveryWindowDays;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}