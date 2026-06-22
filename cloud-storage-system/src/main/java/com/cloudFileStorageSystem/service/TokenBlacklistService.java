package com.cloudFileStorageSystem.service;

public interface TokenBlacklistService {

    void blacklistToken(String token);

    boolean isBlacklisted(String token);
}
