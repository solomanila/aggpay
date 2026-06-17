package com.letsvpn.pay.shopline.util;

import cn.hutool.crypto.SecureUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
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

    public static String buildSortedMessage(Map<String, String> params, String excludeKey) {
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

    // =========================================================
    // RSA 加签 / 验签（Shopline 付款请求 & 回调规范）
    // =========================================================

    /**
     * 构建待签名文本：参数字典升序，标量 key=value，List<scalar> 逗号连接，嵌套 Map/List<Map> 递归展开忽略父 key。
     */
    public static String buildSignatureSourceString(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        appendSignParams(sb, params);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void appendSignParams(StringBuilder sb, Map<String, Object> sourceObj) {
        if (sourceObj == null || sourceObj.isEmpty()) return;
        List<String> keys = new ArrayList<>(sourceObj.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            Object value = sourceObj.get(key);
            if (value == null) continue;
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                if (list.isEmpty()) continue;
                if (isScalarValue(list.get(0))) {
                    if (sb.length() > 0) sb.append("&");
                    sb.append(key).append("=");
                    StringJoiner joiner = new StringJoiner(",");
                    for (Object elem : list) joiner.add(String.valueOf(elem));
                    sb.append(joiner);
                } else {
                    for (Object item : list) {
                        if (item instanceof Map) appendSignParams(sb, (Map<String, Object>) item);
                    }
                }
            } else if (value instanceof Map) {
                appendSignParams(sb, (Map<String, Object>) value);
            } else if (isScalarValue(value)) {
                if (sb.length() > 0) sb.append("&");
                sb.append(key).append("=").append(value);
            }
        }
    }

    private static boolean isScalarValue(Object value) {
        return value instanceof String || value instanceof Integer || value instanceof Long
                || value instanceof Float || value instanceof Double
                || value instanceof BigDecimal || value instanceof Boolean;
    }

    /**
     * 用己方 PKCS8 私钥对参数加签（SHA1withRSA），返回 Base64 签名值。
     * 签名值放入请求头 pay-api-signature。
     */
    public static String signPayRequest(String privateKey, Map<String, Object> params) {
        String source = buildSignatureSourceString(params);
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKey));
            PrivateKey pk = KeyFactory.getInstance("RSA").generatePrivate(spec);
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initSign(pk);
            sig.update(source.getBytes(StandardCharsets.UTF_8));
            return new String(Base64.encodeBase64(sig.sign()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Shopline pay sign failed: " + e.getMessage(), e);
        }
    }

    /**
     * 用 Shopline 公钥验证回调签名（SHA1withRSA）。
     */
    public static boolean verifyPayCallback(String shoplinePublicKey, String signSourceStr, String signedStr) {
        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decodeBase64(shoplinePublicKey));
            PublicKey pk = KeyFactory.getInstance("RSA").generatePublic(spec);
            byte[] signed = Base64.decodeBase64(signedStr);
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(pk);
            sig.update(signSourceStr.getBytes(StandardCharsets.UTF_8));
            return sig.verify(signed);
        } catch (Exception e) {
            log.warn("Shopline pay callback verify failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 验证 Shopline Webhook 推送中 X-Shopline-Hmac-Sha256 请求头的签名。
     * 算法：HMAC-SHA256(rawBody, appSecret)，结果 Base64 编码后与 header 值比对。
     */
    public static boolean verifyWebhookHmac(String rawBody, String signature, String appSecret) {
        if (rawBody == null || signature == null || appSecret == null) return false;
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String encoded = java.util.Base64.getEncoder().encodeToString(computed);
            boolean valid = encoded.equals(signature);
            if (!valid) {
                log.warn("Shopline webhook HMAC mismatch: computed={}, received={}", encoded, signature);
            }
            return valid;
        } catch (Exception e) {
            log.error("Shopline webhook HMAC verify error: {}", e.getMessage());
            return false;
        }
    }
}
