package com.letsvpn.pay.service.core;

import com.letsvpn.pay.exception.WanliException;
import com.letsvpn.pay.util.RedisConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

@Service
public class PayRateLimitService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private DefaultRedisScript<Long> tpsScript;
    private DefaultRedisScript<Long> slidingScript;
    private DefaultRedisScript<Long> idempotencyScript;

    @Value("${pay.ratelimit.global.tps:500}")
    private long defaultGlobalTps;
    @Value("${pay.ratelimit.channel.tps:100}")
    private long defaultChannelTps;
    @Value("${pay.ratelimit.merchant.tps:50}")
    private long defaultMerchantTps;
    @Value("${pay.ratelimit.ip.limit:100}")
    private long defaultIpLimit;
    @Value("${pay.ratelimit.user.limit:20}")
    private long defaultUserLimit;
    @Value("${pay.ratelimit.ip.window:60000}")
    private long ipWindowMs;
    @Value("${pay.ratelimit.user.window:60000}")
    private long userWindowMs;
    @Value("${pay.ratelimit.idempotency.ttl:300}")
    private long idempotencyTtl;

    @PostConstruct
    public void init() {
        tpsScript = new DefaultRedisScript<>();
        tpsScript.setLocation(new ClassPathResource("lua/rate_limit_tps.lua"));
        tpsScript.setResultType(Long.class);

        slidingScript = new DefaultRedisScript<>();
        slidingScript.setLocation(new ClassPathResource("lua/rate_limit_sliding.lua"));
        slidingScript.setResultType(Long.class);

        idempotencyScript = new DefaultRedisScript<>();
        idempotencyScript.setLocation(new ClassPathResource("lua/idempotency_check.lua"));
        idempotencyScript.setResultType(Long.class);
    }

    public void checkIpLimit(String ip) {
        long limit = getLimit(RedisConstant.rlConfigIpLimit(), defaultIpLimit);
        List<String> keys = Collections.singletonList(RedisConstant.rlIp(ip));
        Long result = redisTemplate.execute(slidingScript, keys,
                String.valueOf(limit),
                String.valueOf(ipWindowMs),
                String.valueOf(System.currentTimeMillis()));
        if (result == null || result == 0L) {
            throw new WanliException(1029, "IP请求过于频繁");
        }
    }

    public void checkUserLimit(Long userId) {
        long limit = getLimit(RedisConstant.rlConfigUserLimit(), defaultUserLimit);
        List<String> keys = Collections.singletonList(RedisConstant.rlUser(userId));
        Long result = redisTemplate.execute(slidingScript, keys,
                String.valueOf(limit),
                String.valueOf(userWindowMs),
                String.valueOf(System.currentTimeMillis()));
        if (result == null || result == 0L) {
            throw new WanliException(1030, "用户请求过于频繁");
        }
    }

    public void checkIdempotency(String frontId, int platformId) {
        List<String> keys = Collections.singletonList(RedisConstant.rlIdempotency(platformId, frontId));
        Long result = redisTemplate.execute(idempotencyScript, keys, String.valueOf(idempotencyTtl));
        if (result == null || result == 0L) {
            throw new WanliException(1031, "重复请求" + frontId);
        }
    }

    public void checkMerchantTps(String appId) {
        long limit = getLimit(RedisConstant.rlConfigMerchantTps(appId), defaultMerchantTps);
        List<String> keys = Collections.singletonList(RedisConstant.rlMerchant(appId));
        Long result = redisTemplate.execute(tpsScript, keys, String.valueOf(limit));
        if (result == null || result == 0L) {
            throw new WanliException(1032, "商户请求超限");
        }
    }

    public void checkChannelTps(Integer payConfigId) {
        long limit = getLimit(RedisConstant.rlConfigChannelTps(payConfigId), defaultChannelTps);
        List<String> keys = Collections.singletonList(RedisConstant.rlChannel(payConfigId));
        Long result = redisTemplate.execute(tpsScript, keys, String.valueOf(limit));
        if (result == null || result == 0L) {
            throw new WanliException(1033, "通道请求超限");
        }
    }

    public void checkGlobalTps() {
        long limit = getLimit(RedisConstant.rlConfigGlobalTps(), defaultGlobalTps);
        List<String> keys = Collections.singletonList(RedisConstant.rlGlobal());
        Long result = redisTemplate.execute(tpsScript, keys, String.valueOf(limit));
        if (result == null || result == 0L) {
            throw new WanliException(1034, "系统繁忙请稍后重试");
        }
    }

    private long getLimit(String configKey, long defaultVal) {
        String val = redisTemplate.opsForValue().get(configKey);
        if (val != null) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultVal;
    }
}
