package com.recoveriq.repository;

import com.recoveriq.domain.Experiment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperimentRepository extends JpaRepository<Experiment, Long> {}