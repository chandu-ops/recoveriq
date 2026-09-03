package com.recoveriq.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.recoveriq.domain.RecoveryAction;

public interface RecoveryActionRepository extends JpaRepository<RecoveryAction, Long> {
    Optional<RecoveryAction> findTopByRecoveryCaseIdOrderByCreatedAtDesc(Long recoveryCaseId);
}