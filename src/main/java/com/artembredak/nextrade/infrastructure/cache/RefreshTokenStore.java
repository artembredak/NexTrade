package com.artembredak.nextrade.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    public void store(String token, String email, long ttlMs) {
        redisTemplate.opsForValue().set(PREFIX + token, email, ttlMs, TimeUnit.MILLISECONDS);
    }

    public Optional<String> getEmail(String token) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(PREFIX + token));
    }

    public void delete(String token) {
        redisTemplate.delete(PREFIX + token);
    }
}
