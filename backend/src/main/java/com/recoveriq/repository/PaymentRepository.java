package com.recoveriq.repository;

import com.recoveriq.domain.Payment;
import com.recoveriq.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    long countByCustomerIdAndStatus(Long customerId, PaymentStatus status);
}