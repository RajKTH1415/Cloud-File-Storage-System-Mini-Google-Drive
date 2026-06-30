package com.cloudFileStorageSystem.service.Impl;

import com.cloudFileStorageSystem.module.AuditLog;
import com.cloudFileStorageSystem.repository.AuditLogRepository;
import com.cloudFileStorageSystem.service.AuditService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(
            AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void log(String email, String action, String ip, String device, String details) {

        AuditLog auditLog = new AuditLog();

        auditLog.setIdentifier(email);
        auditLog.setAction(action);
        auditLog.setIpAddress(ip);
        auditLog.setDeviceInfo(device);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setDetails(details);

        auditLogRepository.save(auditLog);
    }
}