package com.letsvpn.admin.service.system.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.letsvpn.admin.constant.SystemCacheKeys;
import com.letsvpn.admin.entity.SystemFeatureFlag;
import com.letsvpn.admin.mapper.SystemFeatureFlagMapper;
import com.letsvpn.admin.service.system.SystemFeatureFlagService;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemFeatureFlagServiceImpl extends ServiceImpl<SystemFeatureFlagMapper, SystemFeatureFlag>
        implements SystemFeatureFlagService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<SystemFeatureFlag> listCached() {
        String cached = stringRedisTemplate.opsForValue().get(SystemCacheKeys.FEATURE_FLAGS_ALL);
        if (StringUtils.hasText(cached)) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<SystemFeatureFlag>>() {});
            } catch (Exception e) {
                log.warn("Failed to deserialize feature flag cache", e);
            }
        }
        List<SystemFeatureFlag> flags = lambdaQuery().list();
        try {
            stringRedisTemplate.opsForValue()
                    .set(SystemCacheKeys.FEATURE_FLAGS_ALL, objectMapper.writeValueAsString(flags), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to serialize feature flag cache", e);
        }
        return flags;
    }

    @Override
    public void evictCache() {
        stringRedisTemplate.delete(SystemCacheKeys.FEATURE_FLAGS_ALL);
    }
}
