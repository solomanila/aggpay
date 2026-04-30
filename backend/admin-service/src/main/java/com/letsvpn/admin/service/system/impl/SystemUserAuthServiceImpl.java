package com.letsvpn.admin.service.system.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.letsvpn.admin.constant.SystemCacheKeys;
import com.letsvpn.admin.entity.SystemUserAuth;
import com.letsvpn.admin.mapper.SystemUserAuthMapper;
import com.letsvpn.admin.service.system.SystemUserAuthService;
import com.letsvpn.admin.util.GoogleAuthenticatorUtil;
import com.letsvpn.common.core.constant.AuthRedisKeys;
import com.letsvpn.common.core.dto.AdminLoginRequest;
import com.letsvpn.common.core.dto.AdminLoginResponse;
import com.letsvpn.common.core.dto.AdminRegisterRequest;
import com.letsvpn.common.core.dto.AdminRegisterResponse;
import com.letsvpn.common.core.util.JwtUtils;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemUserAuthServiceImpl extends ServiceImpl<SystemUserAuthMapper, SystemUserAuth>
        implements SystemUserAuthService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<SystemUserAuth> listCached() {
        String cache = stringRedisTemplate.opsForValue().get(SystemCacheKeys.USER_LIST);
        if (StringUtils.hasText(cache)) {
            try {
                return objectMapper.readValue(cache, new TypeReference<List<SystemUserAuth>>() {});
            } catch (Exception e) {
                log.warn("Failed to deserialize system user cache", e);
            }
        }
        List<SystemUserAuth> result = lambdaQuery().list();
        try {
            stringRedisTemplate.opsForValue()
                    .set(SystemCacheKeys.USER_LIST, objectMapper.writeValueAsString(result), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to serialize system user cache", e);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminRegisterResponse register(AdminRegisterRequest request) {
        long count = lambdaQuery().eq(SystemUserAuth::getAccount, request.getAccount()).count();
        if (count > 0) {
            throw new IllegalArgumentException("账号已存在");
        }
        String secret = StringUtils.hasText(request.getGoogleSecret())
                ? request.getGoogleSecret()
                : GoogleAuthenticatorUtil.generateSecret();
        SystemUserAuth user = new SystemUserAuth();
        user.setAccount(request.getAccount());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setMobile(request.getMobile());
        user.setStatus("ACTIVE");
        user.setRiskLevel("LOW");
        user.setPasswordAlgo("bcrypt");
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setGoogleSecret(secret);
        user.setGoogleEnabled(1);
        user.setTags("[]");
        user.setForceReset(0);
        LocalDateTime now = LocalDateTime.now();
        user.setPasswordUpdatedAt(now);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        save(user);
        evictUserCache();

        AdminRegisterResponse response = new AdminRegisterResponse();
        response.setUserId(user.getId());
        response.setAccount(user.getAccount());
        response.setGoogleSecret(secret);
        response.setOtpAuthUrl(GoogleAuthenticatorUtil.buildOtpAuthUrl(
                user.getAccount(), "PayAdmin", secret));
        return response;
    }

    @Override
    public AdminLoginResponse verifyLogin(AdminLoginRequest request) {
        SystemUserAuth user = lambdaQuery().eq(SystemUserAuth::getAccount, request.getAccount()).one();
        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalStateException("账号状态异常：" + user.getStatus());
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("密码不正确");
        }
        if (user.getGoogleEnabled() != null && user.getGoogleEnabled() == 1) {
            if (!GoogleAuthenticatorUtil.verifyCode(user.getGoogleSecret(), request.getOtpCode())) {
                throw new IllegalArgumentException("动态验证码错误");
            }
            user.setGoogleLastVerifiedAt(LocalDateTime.now());
        }
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(request.getClientIp());
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
        evictUserCache();

        AdminLoginResponse response = new AdminLoginResponse();
        response.setUserId(user.getId());
        response.setAccount(user.getAccount());
        response.setName(user.getName());
        response.setStatus(user.getStatus());
        response.setRiskLevel(user.getRiskLevel());
        response.setTags(parseTags(user.getTags()));
        return response;
    }

    @Override
    public String getOtpAuthUrl(String account) {
        SystemUserAuth user = lambdaQuery().eq(SystemUserAuth::getAccount, account).one();
        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        if (!StringUtils.hasText(user.getGoogleSecret())) {
            throw new IllegalStateException("账号未配置谷歌验证码");
        }
        return GoogleAuthenticatorUtil.buildOtpAuthUrl(user.getAccount(), "PayAdmin", user.getGoogleSecret());
    }

    @Override
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        stringRedisTemplate.delete(AuthRedisKeys.ADMIN_SESSION_PREFIX + token);
        stringRedisTemplate.opsForValue()
                .set(AuthRedisKeys.ADMIN_TOKEN_BLACKLIST_PREFIX + token, "logout",
                        Duration.ofMillis(JwtUtils.EXPIRATION));
    }

    @Override
    public void evictUserCache() {
        stringRedisTemplate.delete(SystemCacheKeys.USER_LIST);
    }

    private List<String> parseTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(tags, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse tags {}", tags, e);
            return Collections.emptyList();
        }
    }
}
