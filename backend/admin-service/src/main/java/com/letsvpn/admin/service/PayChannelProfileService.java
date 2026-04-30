package com.letsvpn.admin.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.letsvpn.admin.entity.PayChannelProfile;
import com.letsvpn.admin.mapper.PayChannelProfileMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayChannelProfileService {

    private static final String REDIS_CHANNEL_TPS_KEY = "payproject:rl:cfg:channel:%d:tps";

    private final PayChannelProfileMapper mapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public PayChannelProfile getById(Long id) {
        return mapper.selectById(id);
    }

    public void upsertLimitConfig(Long id, String limitConfigJson) {
        mapper.update(null, new LambdaUpdateWrapper<PayChannelProfile>()
                .set(PayChannelProfile::getLimitConfig, limitConfigJson)
                .eq(PayChannelProfile::getId, id));

        syncTpsToRedis(id, limitConfigJson);
    }

    private void syncTpsToRedis(Long id, String limitConfigJson) {
        String redisKey = String.format(REDIS_CHANNEL_TPS_KEY, id);
        try {
            JsonNode node = objectMapper.readTree(limitConfigJson);
            JsonNode tpsNode = node.get("tps");
            if (tpsNode != null && tpsNode.isNumber()) {
                long tps = tpsNode.longValue();
                if (tps > 0) {
                    stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(tps));
                    log.info("channel {} tps synced to redis: {}", id, tps);
                } else {
                    stringRedisTemplate.delete(redisKey);
                    log.info("channel {} tps limit removed (tps=0)", id);
                }
            }
        } catch (Exception e) {
            log.error("failed to sync channel tps to redis, id={}, config={}", id, limitConfigJson, e);
        }
    }
}
