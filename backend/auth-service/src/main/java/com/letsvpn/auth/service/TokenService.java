package com.letsvpn.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.letsvpn.common.core.constant.AuthRedisKeys;
import com.letsvpn.common.core.util.JwtUtils;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void cacheAdminSession(String token, Object payload) {
        cache(AuthRedisKeys.ADMIN_SESSION_PREFIX, token, payload);
    }

    public void cacheUserSession(String token, Object payload) {
        cache(AuthRedisKeys.USER_SESSION_PREFIX, token, payload);
    }

    public void blacklistAdminToken(String token) {
        blacklist(AuthRedisKeys.ADMIN_SESSION_PREFIX, AuthRedisKeys.ADMIN_TOKEN_BLACKLIST_PREFIX, token);
    }

    public void blacklistUserToken(String token) {
        blacklist(AuthRedisKeys.USER_SESSION_PREFIX, AuthRedisKeys.USER_TOKEN_BLACKLIST_PREFIX, token);
    }

    private void cache(String prefix, String token, Object payload) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue()
                    .set(prefix + token, objectMapper.writeValueAsString(payload),
                            Duration.ofMillis(JwtUtils.EXPIRATION));
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache auth session", e);
        }
    }

    private void blacklist(String sessionPrefix, String blacklistPrefix, String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        stringRedisTemplate.delete(sessionPrefix + token);
        stringRedisTemplate.opsForValue()
                .set(blacklistPrefix + token, "1", Duration.ofMillis(JwtUtils.EXPIRATION));
    }
}
