package com.recoveriq.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.recoveriq.domain.Policy;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
    Optional<Policy> findFirstByActiveTrue();
}