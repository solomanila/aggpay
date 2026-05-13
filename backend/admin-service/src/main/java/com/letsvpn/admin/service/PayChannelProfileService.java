package com.letsvpn.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.letsvpn.admin.dto.ChannelProfilePageRequest;
import com.letsvpn.admin.dto.ChannelProfileVO;
import com.letsvpn.admin.entity.PayChannelProfile;
import com.letsvpn.admin.mapper.PayChannelProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public IPage<ChannelProfileVO> pageList(ChannelProfilePageRequest req) {
        Page<PayChannelProfile> page = new Page<>(req.getPageNum(), req.getPageSize());
        LambdaQueryWrapper<PayChannelProfile> wrapper = new LambdaQueryWrapper<PayChannelProfile>()
                .eq(StringUtils.hasText(req.getAreaType()), PayChannelProfile::getAreaType, req.getAreaType())
                // business_types 是 JSON 数组，用 LIKE '%"VALUE"%' 匹配数组元素
                .like(StringUtils.hasText(req.getBusinessType()), PayChannelProfile::getBusinessTypes,
                        "\"" + req.getBusinessType() + "\"")
                .orderByDesc(PayChannelProfile::getId);
        IPage<PayChannelProfile> rawPage = mapper.selectPage(page, wrapper);
        return rawPage.convert(this::toVO);
    }

    public Map<String, List<String>> options() {
        List<String> areaTypes = mapper.selectDistinctAreaTypes();

        // 每行是一个 JSON 数组字符串，如 ["PIX","PAYIN"]，拍平后去重
        List<String> rawJsonArrays = mapper.selectDistinctBusinessTypes();
        List<String> businessTypes = rawJsonArrays.stream()
                .flatMap(json -> {
                    try {
                        List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
                        return list.stream();
                    } catch (Exception e) {
                        return Stream.empty();
                    }
                })
                .filter(s -> s != null && !s.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        Map<String, List<String>> result = new HashMap<>();
        result.put("areaTypes", areaTypes);
        result.put("businessTypes", businessTypes);
        return result;
    }

    public void upsertLimitConfig(Long id, String limitConfigJson) {
        mapper.update(null, new LambdaUpdateWrapper<PayChannelProfile>()
                .set(PayChannelProfile::getLimitConfig, limitConfigJson)
                .eq(PayChannelProfile::getId, id));
        syncTpsToRedis(id, limitConfigJson);
    }

    private ChannelProfileVO toVO(PayChannelProfile p) {
        ChannelProfileVO vo = new ChannelProfileVO();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setAreaType(p.getAreaType());
        vo.setFeeRate(p.getFeeRate());
        vo.setCostRate(p.getCostRate());

        // 解析 business_types JSON 数组，如 ["PIX","PAYIN"]
        if (StringUtils.hasText(p.getBusinessTypes())) {
            try {
                vo.setBusinessTypes(objectMapper.readValue(p.getBusinessTypes(), new TypeReference<List<String>>() {}));
            } catch (Exception e) {
                log.warn("Failed to parse business_types for channel profile id={}", p.getId());
                vo.setBusinessTypes(Collections.emptyList());
            }
        } else {
            vo.setBusinessTypes(Collections.emptyList());
        }

        // 解析 extra JSON 对象
        if (StringUtils.hasText(p.getExtra())) {
            try {
                vo.setExtra(objectMapper.readValue(p.getExtra(), new TypeReference<Map<String, Object>>() {}));
            } catch (Exception e) {
                log.warn("Failed to parse extra for channel profile id={}", p.getId());
                vo.setExtra(Collections.emptyMap());
            }
        } else {
            vo.setExtra(Collections.emptyMap());
        }

        return vo;
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
                } else {
                    stringRedisTemplate.delete(redisKey);
                }
            }
        } catch (Exception e) {
            log.error("failed to sync channel tps to redis, id={}, config={}", id, limitConfigJson, e);
        }
    }
}
