package com.letsvpn.pay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.letsvpn.common.core.dto.MerchantChannelConfigDTO;
import com.letsvpn.common.core.response.R;
import com.letsvpn.pay.client.AdminServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceFacade {

    private static final String CACHE_PREFIX = "mcc:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final AdminServiceClient adminServiceClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /** 获取商户通道配置列表，Redis 缓存 60s。admin-service 不可达时返回空列表（降级放行）。 */
    public List<MerchantChannelConfigDTO> fetchMerchantChannelConfigs(Integer platformId, String channelType) {
        String key = CACHE_PREFIX + platformId + ":" + channelType;
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(cached)) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<MerchantChannelConfigDTO>>() {});
            } catch (IOException e) {
                log.warn("Failed to deserialize cached merchant channel configs", e);
            }
        }

        R<List<MerchantChannelConfigDTO>> resp =
                adminServiceClient.getMerchantChannelConfigs(platformId, channelType);
        List<MerchantChannelConfigDTO> data =
                (resp != null && R.isSuccess(resp.getCode()) && resp.getData() != null)
                        ? resp.getData() : Collections.emptyList();

        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(data), CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize merchant channel configs for cache", e);
        }
        return data;
    }

    /** 禁用通道配置（日限额触发），同时清除缓存使下次请求立即感知。 */
    public void disableMerchantChannelConfig(Long id, Integer platformId, String channelType) {
        try {
            adminServiceClient.disableMerchantChannelConfig(id);
        } catch (Exception e) {
            log.warn("Failed to disable merchant channel config id={}", id, e);
        }
        stringRedisTemplate.delete(CACHE_PREFIX + platformId + ":" + channelType);
    }
}
