package com.basilcode.emsbackend.auth.service;

import com.basilcode.emsbackend.common.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Fixed-window per-email counter in Redis, mirroring {@code OtpService}'s attempt limiting
 * (same MAX_ATTEMPTS, same lock-until-window-expires shape) since login had none.
 */
@Component
@RequiredArgsConstructor
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    public void checkAllowed(String email) {
        String value = redisTemplate.opsForValue().get(key(email));
        int attempts = value != null ? Integer.parseInt(value) : 0;
        if (attempts >= MAX_ATTEMPTS) {
            throw new TooManyRequestsException("Too many failed login attempts. Please try again in a few minutes.");
        }
    }

    public void recordFailure(String email) {
        String redisKey = key(email);
        Long attempts = redisTemplate.opsForValue().increment(redisKey);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(redisKey, WINDOW);
        }
    }

    public void recordSuccess(String email) {
        redisTemplate.delete(key(email));
    }

    private String key(String email) {
        return "login:attempts:" + email.toLowerCase();
    }
}
