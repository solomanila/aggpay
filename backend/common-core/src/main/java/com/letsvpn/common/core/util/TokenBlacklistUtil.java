package com.letsvpn.common.core.util;

import com.letsvpn.common.core.constant.AuthRedisKeys;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class TokenBlacklistUtil {

    private final StringRedisTemplate redisTemplate;

    public void blacklistAdminToken(String token, long expireMillis) {
        blacklist(AuthRedisKeys.ADMIN_TOKEN_BLACKLIST_PREFIX, token, expireMillis);
    }

    public void blacklistUserToken(String token, long expireMillis) {
        blacklist(AuthRedisKeys.USER_TOKEN_BLACKLIST_PREFIX, token, expireMillis);
    }

    public boolean isAdminTokenBlacklisted(String token) {
        return isBlacklisted(AuthRedisKeys.ADMIN_TOKEN_BLACKLIST_PREFIX, token);
    }

    public boolean isUserTokenBlacklisted(String token) {
        return isBlacklisted(AuthRedisKeys.USER_TOKEN_BLACKLIST_PREFIX, token);
    }

    private void blacklist(String prefix, String token, long expireMillis) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        redisTemplate.opsForValue().set(prefix + token, "1", expireMillis, TimeUnit.MILLISECONDS);
        redisTemplate.expire(prefix + token, expireMillis, TimeUnit.MILLISECONDS);
    }

    private boolean isBlacklisted(String prefix, String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(prefix + token));
    }
}
