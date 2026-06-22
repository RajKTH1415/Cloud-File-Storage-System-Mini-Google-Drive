package com.cloudFileStorageSystem.service.Impl;

import com.cloudFileStorageSystem.module.TokenBlacklist;
import com.cloudFileStorageSystem.repository.TokenBlacklistRepository;
import com.cloudFileStorageSystem.service.TokenBlacklistService;
import com.cloudFileStorageSystem.util.JwtUtil;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtUtil jwtUtil;

    public TokenBlacklistServiceImpl(TokenBlacklistRepository tokenBlacklistRepository, JwtUtil jwtUtil) {
        this.tokenBlacklistRepository = tokenBlacklistRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void blacklistToken(String token) {

        String jti = jwtUtil.extractJti(token);
        Date expiry = jwtUtil.extractExpiration(token);

        TokenBlacklist blacklisted = new TokenBlacklist();
        blacklisted.setJti(jti);
        blacklisted.setExpiryDate(expiry);

        tokenBlacklistRepository.save(blacklisted);
    }

    @Override
    public boolean isBlacklisted(String token) {
        return tokenBlacklistRepository.existsByJti(jwtUtil.extractJti(token));
    }
}
