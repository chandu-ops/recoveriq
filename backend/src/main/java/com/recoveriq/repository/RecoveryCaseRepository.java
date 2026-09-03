package com.recoveriq.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.recoveriq.domain.RecoveryCase;
import com.recoveriq.enums.CaseStatus;

public interface RecoveryCaseRepository extends JpaRepository<RecoveryCase, Long> {
    List<RecoveryCase> findByStatus(CaseStatus status);
    List<RecoveryCase> findAllByOrderByExpectedRecoveryDesc();
    @org.springframework.data.jpa.repository.Query(
            "SELECT rc FROM RecoveryCase rc JOIN FETCH rc.customer " +
            "WHERE rc.status = 'OPEN' ORDER BY rc.expectedRecovery DESC")
        List<RecoveryCase> findTop50OpenOrderedByExpectedRecovery(org.springframework.data.domain.Pageable pageable);
}
