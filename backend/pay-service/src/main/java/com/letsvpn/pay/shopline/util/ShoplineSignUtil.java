package com.letsvpn.pay.shopline.util;

import cn.hutool.crypto.SecureUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
public class ShoplineSignUtil {

    /**
     * 验证 Shopline 下发请求的签名（适用于 install、callback 等接口）。
     * 算法：将全部参数（排除 sign 本身）按 key 升序排列，拼接为 key=value&key=value，
     * 再以 AppSecret 做 HMAC-SHA256，结果（十六进制小写）与 sign 参数对比。
     */
    public static boolean verifyIncoming(Map<String, String> params, String sign, String appSecret) {
        if (sign == null || appSecret == null) return false;
        String message = buildSortedMessage(params, "sign");
        String computed = SecureUtil.hmacSha256(appSecret).digestHex(message);
        boolean valid = computed.equalsIgnoreCase(sign);
        if (!valid) {
            log.warn("Shopline sign mismatch: message={}, computed={}, received={}", message, computed, sign);
        }
        return valid;
    }

    /**
     * 为发往 Shopline 的请求（token/create、token/refresh）计算签名。
     * 传入业务参数 map（appkey、handle、timestamp 等），返回 HMAC-SHA256 hex。
     */
    public static String buildOutgoing(Map<String, String> params, String appSecret) {
        String message = buildSortedMessage(params, null);
        return SecureUtil.hmacSha256(appSecret).digestHex(message);
    }

    /**
     * 为 POST 请求计算签名，返回需要添加到请求头的 sign 和 timestamp。
     * source = body + timestamp（13位毫秒时间戳），sign = HMAC-SHA256(source, appSecret)。
     *
     * @param body      请求 Body 字符串，为空时传空字符串
     * @param appSecret 应用密钥
     * @return 包含 "sign" 和 "timestamp" 的 map，直接写入请求 Header
     */
    public static String buildOutgoingPost(String body, String appSecret,String timestamp) {
        String source = (body == null ? "" : body) + timestamp;
        String sign = SecureUtil.hmacSha256(appSecret).digestHex(source);
        return sign;
    }

    private static String buildSortedMessage(Map<String, String> params, String excludeKey) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(e -> !e.getKey().equals(excludeKey))
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    /**
     * 验证 Shopline Webhook 的 RSA-SHA256 签名（使用 Shopline 提供的公钥）。
     */
    public static boolean verifyWebhookSignature(String data, String sign, String publicKey) {
        try {
            return com.letsvpn.pay.third.util.SHA256WithRSAUtils.buildRSAverifyByPublicKey(data, publicKey, sign);
        } catch (Exception e) {
            log.error("Shopline webhook RSA verify failed: {}", e.getMessage());
            return false;
        }
    }
}
