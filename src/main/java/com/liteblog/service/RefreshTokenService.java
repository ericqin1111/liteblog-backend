package com.liteblog.service;

public interface RefreshTokenService {

    void store(String jti, String username, String sessionId, long ttlMillis);

    boolean isValid(String jti, String username, String sessionId);

    void revoke(String jti);
}
