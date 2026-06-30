package com.cloudFileStorageSystem.service;

public interface AuditService {

    void log(String identifier, String action, String ip, String device, String details);
}
