package com.letsvpn.pay.shopline.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.letsvpn.pay.service.AdminServiceFacade;
import com.letsvpn.pay.shopline.config.ShoplineConfig;
import com.letsvpn.pay.shopline.entity.ShoplineShopToken;
import com.letsvpn.pay.shopline.mapper.ShoplineShopTokenMapper;
import com.letsvpn.pay.shopline.util.ShoplineSignUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShoplineOAuthService {

    private final ShoplineConfig shoplineConfig;
    private final ShoplineShopTokenMapper shoplineShopTokenMapper;
    private final AdminServiceFacade adminServiceFacade;

    private static final DateTimeFormatter EXPIRE_FORMATTER = new DateTimeFormatterBuilder()
            .appendOptional(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .appendOptional(DateTimeFormatter.ISO_DATE_TIME)
            .toFormatter();

    /**
     * 按 handle 查询店铺 token（handle 即 Shopline 回调中的 handle 参数）。
     */
    public ShoplineShopToken getByHandle(String handle) {
        QueryWrapper<ShoplineShopToken> wrapper = new QueryWrapper<>();
        wrapper.eq("shop_handle", handle);
        return shoplineShopTokenMapper.selectOne(wrapper);
    }

    /**
     * 按 shopId 查询 token。
     */
    public ShoplineShopToken getByShopId(String shopId) {
        QueryWrapper<ShoplineShopToken> wrapper = new QueryWrapper<>();
        wrapper.eq("shop_id", shopId);
        return shoplineShopTokenMapper.selectOne(wrapper);
    }

    /**
     * 判断 token 是否已过期（含 5 分钟缓冲）。
     */
    public boolean isExpired(ShoplineShopToken token) {
        if (token == null || token.getExpiresAt() == null) return true;
        return token.getExpiresAt().getTime() - System.currentTimeMillis() < 5 * 60 * 1000L;
    }

    /**
     * 用授权码向 Shopline 换取 access_token（POST /admin/oauth/token/create），并持久化。
     * 请求头：appkey、timestamp、sign（HMAC over {appkey, code, handle, timestamp}）
     * 请求体：{"code": code}
     * 成功条件：HTTP 200 && code==200 && i18nCode=="SUCCESS"
     */
    public ShoplineShopToken createTokenFromCode(String handle, String code) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        Map<String, String> signParams = new TreeMap<>();
        signParams.put("appkey", shoplineConfig.getAppKey());
        signParams.put("code", code);
        //signParams.put("handle", handle);
        signParams.put("timestamp", timestamp);
        //String sign = ShoplineSignUtil.buildOutgoing(signParams, shoplineConfig.getAppSecret());

        String url = "https://" + handle + ".myshopline.com/admin/oauth/token/create";
        String reqBody = JSONUtil.createObj().set("code", code).toString();

        String sign = ShoplineSignUtil.buildOutgoingPost(reqBody, shoplineConfig.getAppSecret(),timestamp);



        log.info("Shopline token/create: handle={}", handle);
        String responseText;
        try {
            HttpResponse resp = HttpRequest.post(url)
                    .header("appkey", shoplineConfig.getAppKey())
                    .header("timestamp", timestamp)
                    .header("sign", sign)
                    .header("Content-Type", "application/json")
                    .body(reqBody)
                    .timeout(15000)
                    .execute();
            responseText = resp.body();
        } catch (Exception e) {
            log.error("Shopline token/create failed: handle={}, err={}", handle, e.getMessage(), e);
            return null;
        }

        log.info("Shopline token/create response: {}", responseText);
        JSONObject resp = JSONUtil.parseObj(responseText);
        if (resp.getInt("code", -1) != 200 || !"SUCCESS".equals(resp.getStr("i18nCode"))) {
            log.error("Shopline token/create business error: handle={}, resp={}", handle, responseText);
            return null;
        }

        JSONObject data = resp.getJSONObject("data");
        String accessToken = data.getStr("accessToken");
        String refreshToken = data.getStr("refreshToken");
        String expireTimeStr = data.getStr("expireTime");
        Date expiresAt = parseExpireTime(expireTimeStr);

        return saveOrUpdateToken(handle, handle, accessToken, refreshToken,
                shoplineConfig.getScopes(), expiresAt);
    }

    /**
     * 刷新已有的 access_token（POST /admin/oauth/token/refresh）。
     * 请求头：appkey、timestamp、sign（HMAC over {appkey, handle, timestamp}）
     * 无请求体。
     */
    public ShoplineShopToken refreshToken(String handle) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        Map<String, String> signParams = new TreeMap<>();
        signParams.put("appkey", shoplineConfig.getAppKey());
        signParams.put("handle", handle);
        signParams.put("timestamp", timestamp);
        String sign = ShoplineSignUtil.buildOutgoing(signParams, shoplineConfig.getAppSecret());

        String url = "https://" + handle + ".myshopline.com/admin/oauth/token/refresh";

        log.info("Shopline token/refresh: handle={}", handle);
        String responseText;
        try {
            HttpResponse resp = HttpRequest.post(url)
                    .header("appkey", shoplineConfig.getAppKey())
                    .header("timestamp", timestamp)
                    .header("sign", sign)
                    .timeout(15000)
                    .execute();
            responseText = resp.body();
        } catch (Exception e) {
            log.error("Shopline token/refresh failed: handle={}, err={}", handle, e.getMessage(), e);
            return null;
        }

        log.info("Shopline token/refresh response: {}", responseText);
        JSONObject resp = JSONUtil.parseObj(responseText);
        if (resp.getInt("code", -1) != 200 || !"SUCCESS".equals(resp.getStr("i18nCode"))) {
            log.error("Shopline token/refresh business error: handle={}, resp={}", handle, responseText);
            return null;
        }

        JSONObject data = resp.getJSONObject("data");
        String accessToken = data.getStr("accessToken");
        String refreshToken = data.getStr("refreshToken");
        String expireTimeStr = data.getStr("expireTime");
        Date expiresAt = parseExpireTime(expireTimeStr);

        return saveOrUpdateToken(handle, handle, accessToken, refreshToken,
                shoplineConfig.getScopes(), expiresAt);
    }

    /**
     * 持久化或更新店铺 token。
     */
    public ShoplineShopToken saveOrUpdateToken(String shopId, String shopHandle,
                                                String accessToken, String refreshToken,
                                                String scope, Date expiresAt) {
        Date now = new Date();
        QueryWrapper<ShoplineShopToken> qw = new QueryWrapper<>();
        qw.eq("shop_handle", shopHandle);
        ShoplineShopToken existing = shoplineShopTokenMapper.selectOne(qw);

        String platformNo = adminServiceFacade.createMerchantForShopline(shopHandle);

        if (existing == null) {
            ShoplineShopToken token = new ShoplineShopToken()
                    .setShopId(platformNo)
                    .setShopHandle(shopHandle)
                    .setAccessToken(accessToken)
                    .setRefreshToken(refreshToken)
                    .setScope(scope)
                    .setExpiresAt(expiresAt)
                    .setCreateTime(now)
                    .setUpdateTime(now);
            shoplineShopTokenMapper.insert(token);
            log.info("Shopline token saved: shopHandle={}", shopHandle);
            return token;
        }

        UpdateWrapper<ShoplineShopToken> uw = new UpdateWrapper<>();
        uw.eq("shop_handle", shopHandle);
        ShoplineShopToken update = new ShoplineShopToken()
                .setAccessToken(accessToken)
                .setScope(scope)
                .setExpiresAt(expiresAt)
                .setUpdateTime(now);
        if (refreshToken != null) {
            update.setRefreshToken(refreshToken);
        }
        shoplineShopTokenMapper.update(update, uw);
        log.info("Shopline token updated: shopHandle={}", shopHandle);
        existing.setAccessToken(accessToken);
        return existing;
    }

    /**
     * 解析 Shopline 返回的 expireTime（ISO 8601 格式，如 2024-01-01T00:00:00.000+00:00）。
     */
    public Date parseExpireTime(String expireTimeStr) {
        if (expireTimeStr == null) return null;
        try {
            OffsetDateTime odt = OffsetDateTime.parse(expireTimeStr, EXPIRE_FORMATTER);
            return Date.from(odt.toInstant());
        } catch (Exception e) {
            log.warn("Failed to parse expireTime: {}", expireTimeStr);
            return null;
        }
    }

    /**
     * 按 handle 删除店铺 token，返回删除行数。
     */
    public int deleteByHandle(String handle) {
        QueryWrapper<ShoplineShopToken> qw = new QueryWrapper<>();
        qw.eq("shop_handle", handle);
        int rows = shoplineShopTokenMapper.delete(qw);
        log.info("Shopline token deleted: shopHandle={}, rows={}", handle, rows);
        return rows;
    }

    /**
     * 从店铺域名中提取 shopId（取 . 之前的部分）。
     */
    public String extractShopId(String shop) {
        if (shop == null) return null;
        int dot = shop.indexOf('.');
        return dot > 0 ? shop.substring(0, dot) : shop;
    }
}
