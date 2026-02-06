package com.liteblog.service.impl;

import com.liteblog.service.RefreshTokenService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void store(String jti, String username, String sessionId, long ttlMillis) {
        String key = buildKey(jti);
        String value = buildValue(username, sessionId);
        redisTemplate.opsForValue().set(key, value, Duration.ofMillis(ttlMillis));
    }

    @Override
    public boolean isValid(String jti, String username, String sessionId) {
        String stored = redisTemplate.opsForValue().get(buildKey(jti));
        if (stored == null) {
            return false;
        }
        return stored.equals(buildValue(username, sessionId));
    }

    @Override
    public void revoke(String jti) {
        redisTemplate.delete(buildKey(jti));
    }

    private String buildKey(String jti) {
        return KEY_PREFIX + jti;
    }

    private String buildValue(String username, String sessionId) {
        return username + ":" + sessionId;
    }
}
