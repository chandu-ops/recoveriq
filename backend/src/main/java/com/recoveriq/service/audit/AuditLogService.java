package com.recoveriq.service.audit;

import org.springframework.stereotype.Service;
import com.recoveriq.domain.AuditLog;
import com.recoveriq.domain.RecoveryCase;
import com.recoveriq.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public void log(RecoveryCase rc, String eventType, String detail) {
        auditLogRepository.save(AuditLog.builder()
                .recoveryCase(rc).eventType(eventType).eventDetail(detail).build());
    }
}