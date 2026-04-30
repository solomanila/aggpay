package com.letsvpn.admin.service.system.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.letsvpn.admin.constant.SystemCacheKeys;
import com.letsvpn.admin.entity.SystemSetting;
import com.letsvpn.admin.entity.SystemSettingAudit;
import com.letsvpn.admin.mapper.SystemSettingMapper;
import com.letsvpn.admin.service.system.SystemSettingAuditService;
import com.letsvpn.admin.service.system.SystemSettingService;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSettingServiceImpl extends ServiceImpl<SystemSettingMapper, SystemSetting>
        implements SystemSettingService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SystemSettingAuditService auditService;

    @Override
    public List<SystemSetting> listCached() {
        String cached = stringRedisTemplate.opsForValue().get(SystemCacheKeys.SETTINGS_ALL);
        if (StringUtils.hasText(cached)) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<SystemSetting>>() {});
            } catch (Exception e) {
                log.warn("Failed to deserialize system settings cache", e);
            }
        }
        List<SystemSetting> settings = lambdaQuery().list();
        try {
            stringRedisTemplate.opsForValue()
                    .set(SystemCacheKeys.SETTINGS_ALL, objectMapper.writeValueAsString(settings), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to serialize system settings cache", e);
        }
        return settings;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemSetting saveOrUpdateSetting(SystemSetting setting, Long operatorId, String changeType) {
        if (setting.getId() == null) {
            save(setting);
        } else {
            updateById(setting);
        }
        SystemSettingAudit audit = new SystemSettingAudit();
        audit.setSettingId(setting.getId());
        SystemSettingAudit latest = auditService.lambdaQuery()
                .eq(SystemSettingAudit::getSettingId, setting.getId())
                .orderByDesc(SystemSettingAudit::getVersion)
                .last("limit 1")
                .one();
        int nextVersion = latest == null ? 1 : latest.getVersion() + 1;
        audit.setVersion(nextVersion);
        audit.setValueSnapshot(setting.getValue());
        audit.setChangeType(changeType);
        audit.setOperatorUserId(operatorId);
        auditService.save(audit);
        evictCache();
        return setting;
    }

    @Override
    public void evictCache() {
        stringRedisTemplate.delete(SystemCacheKeys.SETTINGS_ALL);
    }
}
