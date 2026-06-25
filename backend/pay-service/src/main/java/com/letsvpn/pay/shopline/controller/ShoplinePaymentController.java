package com.letsvpn.pay.shopline.controller;

import cn.hutool.json.JSONUtil;
import com.letsvpn.pay.shopline.config.ShoplineConfig;
import com.letsvpn.pay.shopline.dto.ShoplinePayRequest;
import com.letsvpn.pay.shopline.dto.ShoplinePayResponse;
import com.letsvpn.pay.shopline.dto.ShoplineQueryResponse;
import com.letsvpn.pay.shopline.service.ShoplinePaymentService;
import com.letsvpn.pay.shopline.util.ShoplineSignUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Shopline 付款接口。
 * 外部路径（经网关）：POST /api/pay/shopline/pay
 *
 * 安全层级：
 *  1. pay-api-signature 验签：HMAC-SHA256(rawBody + timestamp, appSecret)
 *  2. pay-api-idempotency-key 幂等：同 key 直接返回 SUCCESS 占位（Shopline 规范要求）
 *  3. storeHandle → platformNo DB 映射，平台不存在则拒绝
 */
@RestController
@RequestMapping("/pay/shopline")
@RequiredArgsConstructor
@Slf4j
public class ShoplinePaymentController {

    private final ShoplineConfig shoplineConfig;
    private final ShoplinePaymentService shoplinePaymentService;

    @PostMapping("/pay")
    public ResponseEntity<ShoplinePayResponse> pay(
            @RequestHeader("pay-api-signature")      String signature,
            @RequestHeader("pay-api-timestamp")      String timestamp,
            @RequestHeader("pay-api-idempotency-key") String idempotencyKey,
            @RequestHeader("pay-api-store-handle")   String storeHandle,
            @RequestBody String rawBody,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.info("shopline pay request: handle={} idempotencyKey={}", storeHandle, idempotencyKey);
        log.info("shopline pay rawBody: handle={} body={}", storeHandle, rawBody);

        // 1. 将原始报文解析为 Map，保留所有字段（含 Shopline 注入的随机键值对）
        cn.hutool.json.JSONObject rawParams;
        try {
            rawParams = JSONUtil.parseObj(rawBody);
        } catch (Exception e) {
            log.warn("shopline pay: parse body failed handle={} err={}", storeHandle, e.getMessage());
            return buildResponse(rejectResponse(null, "请求体解析失败"));
        }

        // 2. 验签（RSA SHA1withRSA，Shopline 公钥）；必须在业务解析前完成
        String signSource = ShoplineSignUtil.buildSignatureSourceString(rawParams);
        if (!ShoplineSignUtil.verifyPayCallback(shoplineConfig.getPublicKey(), signSource, signature)) {
            log.warn("shopline pay: RSA signature mismatch handle={}", storeHandle);
            return buildResponse(rejectResponse(null, "签名验证失败"));
        }



        // 3. 验签通过后再做业务结构化解析
        ShoplinePayRequest payRequest;
        try {
            payRequest = JSONUtil.toBean(rawBody, ShoplinePayRequest.class);
        } catch (Exception e) {
            log.warn("shopline pay: parse body failed handle={} err={}", storeHandle, e.getMessage());
            return buildResponse(rejectResponse(null, "请求体解析失败"));
        }

        // 3. 提取 notifyUrl 存入 extend3
        String extend3 = rawParams.getStr("notifyUrl");

        // 4. 执行下单
        String clientIp = getClientIp(request);
        ShoplinePayResponse resp = shoplinePaymentService.pay(
                storeHandle, payRequest, clientIp, request, response, extend3);

        return buildResponse(resp);
    }

    @PostMapping("/query")
    public ResponseEntity<ShoplineQueryResponse> query(
            @RequestHeader("pay-api-signature")       String signature,
            @RequestHeader("pay-api-timestamp")       String timestamp,
            @RequestHeader("pay-api-idempotency-key") String idempotencyKey,
            @RequestBody String rawBody) {

        log.info("shopline query request: idempotencyKey={}", idempotencyKey);

        // 1. 将原始报文解析为 Map，保留所有字段（含 Shopline 注入的随机键值对）
        cn.hutool.json.JSONObject rawParams;
        try {
            rawParams = JSONUtil.parseObj(rawBody);
        } catch (Exception e) {
            log.warn("shopline query: parse body failed err={}", e.getMessage());
            return buildQueryResponse(queryFail(null, "请求体解析失败"));
        }

        // 2. 验签（RSA SHA1withRSA，Shopline 公钥）；必须在业务解析前完成
        String signSource = ShoplineSignUtil.buildSignatureSourceString(rawParams);
        if (!ShoplineSignUtil.verifyPayCallback(shoplineConfig.getPublicKey(), signSource, signature)) {
            log.warn("shopline query: RSA signature mismatch");
            return buildQueryResponse(queryFail(null, "签名验证失败"));
        }

        // 3. 执行查询
        String orderTransactionId = rawParams.getStr("orderTransactionId");
        ShoplineQueryResponse resp = shoplinePaymentService.query(orderTransactionId);
        return buildQueryResponse(resp);
    }

    private ResponseEntity<ShoplineQueryResponse> buildQueryResponse(ShoplineQueryResponse resp) {
        cn.hutool.json.JSONObject respMap = JSONUtil.parseObj(JSONUtil.toJsonStr(resp));
        String respSign = ShoplineSignUtil.signPayRequest(shoplineConfig.getResponsePrivateKey(), respMap);
        return ResponseEntity.ok()
                .header("pay-api-signature", respSign)
                .body(resp);
    }

    private ShoplineQueryResponse queryFail(String orderTransactionId, String message) {
        ShoplineQueryResponse resp = new ShoplineQueryResponse();
        resp.setReturnCode("FAIL");
        resp.setReturnMessage(message);
        resp.setReturnMessageId(UUID.randomUUID().toString());
        resp.setOrderTransactionId(orderTransactionId != null ? orderTransactionId : "");
        resp.setChannelOrderTransactionId("");
        return resp;
    }

    // 构造响应：body + pay-api-signature 响应头（SHA1withRSA，己方私钥）
    private ResponseEntity<ShoplinePayResponse> buildResponse(ShoplinePayResponse resp) {
        cn.hutool.json.JSONObject respMap = JSONUtil.parseObj(JSONUtil.toJsonStr(resp));
        String respSign = ShoplineSignUtil.signPayRequest(shoplineConfig.getResponsePrivateKey(), respMap);
        return ResponseEntity.ok()
                .header("pay-api-signature", respSign)
                .body(resp);
    }

    private ShoplinePayResponse rejectResponse(String orderTransactionId, String message) {
        ShoplinePayResponse resp = new ShoplinePayResponse();
        resp.setReturnCode("FAIL");
        resp.setReturnMessage(message);
        resp.setReturnMessageId(UUID.randomUUID().toString());
        resp.setOrderTransactionId(orderTransactionId != null ? orderTransactionId : "");
        resp.setChannelOrderTransactionId("");
        return resp;
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // =========================================================
    // RSA 加签 / 验签（Shopline 付款请求 & 回调规范）
    // =========================================================

    /**
     * 演示入口：加签 + 验签流程。
     * privateKey：己方 PKCS8 私钥（Base64）；publicKey：Shopline 公钥（Base64 X.509）。
     */
    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException {
        String privateKey = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDO/TD8axRYRHG+OE6WIFuHH6tyovmqVBw0J5omnvp3RiK1iCnzoJ7M11mK+a0xCshZst1gtMTI+KDPC2OYJMOjckAKSSZtM0FEm2Saq9ZyCWvJady2YsFQgaHtpsJA1YGe18f7sxsn5aCfdsQZ5Zr21OOzSD/yhS9gbIgDYQg0lTtW8w/Q6kdJgRLJfb4i01gKCseBk0v+YRDSb3FCKL0jcWMvdRVySCSXH2CC20udYNOhFSHpbfbu4zGmCAAQKhKVanFJWJYxoE3YKIKIZZ0/BqXIdY4zBQvnKY71cE35/IXDcqdlPLw6uLVzmHbe6ESWmFqc8K4UbebP6AZslvVDAgMBAAECggEARChDFRr9XANNoOFRn0WQXfIjtl/L7jY8A0B3FfiLaPMDQttPVRWytKjEp2qHiPqP9llbZBiiI7sa2JJbfSG9+fgI9loX+SVDfJ/ehL/IoUxQ3cWqE7R5C1VkSKj8lfS39eH/y/WyH3URavDkqdJdMKHxSHul8unAK/QQ5WvyE73oZos8AWRa9VWKiXnw/kJrBFfazsxvqXSWE3pNGHP4MJfuxYHGKY2bC9Z8FVKdK5h71vW1q+TPPGTZAcAoZztR/VbB34SiUaUK5NwLiRT7zXY92f8LeUF0ChizO5gZGypkhwoODdCuR4Htw7otap2eBWnFl5tfLAfUicoV/GLLgQKBgQDrDMJ/JfGol5C2OOk2RfiEiG9aWkvF+Rr3DRIPC1i5KuZcCXpZjTgn6xZDvbRkry5hcXjjy3lzOlP2hzfbF1Gyaw1LtiWEB5x3Zvu9ddKlavdUIotv+1F3ekCP1QnQiIpGq4s/qQ6PLtnrkq8e+pT+s+jX+LpJw/xmLNwYezzaUQKBgQDhcCe1e5AgvTn1y/vNkPyw+fUGS35lQblRbj1Y4YelA5kImi5pkx3iFQb6XsR1Wmn4hv9QRZhTq5cXW9JZXZZzhI9cEdFAfz7EDkyClHE4PP96dBR5gqniZLE5ubk8oiMmEcytWQWmwb+xX7/nnNPby7i6SRguCXfOiGUwJ9EdUwKBgHqDERFSxq2YKk5ARZYLTGhP8LJIZDxRBbQFNQdwY5NH//+y1Pm+OKndx6IRS+g6wtL5YQhicvATU9YoTn28ntF/KNPwoYc6rFwz6jyrH9smcLmCs+jvNlwu9V3CrbXqpSAGo7LPvA33XpCByRM9itFjFpcTRo3SQElFUobUHTixAoGAKSCA3gbwwEhFLqZMBbCRqOew37keEfLvj/+AiZp7WItTe9JE7VW9eeVEJKDtTkt0UbavFUHdDEadhdFmio8cR27DiJRnjFCqbrH9G0VhclUOdpR+t7wyqe6ctl8/f+REbUmKAYXgFg/6nK1PIT3nGI4N4U48bwmjJbaUXzikWakCgYEAsmUH21UvEZ6I6xS84U/0kol0s/wVN36skItocI00lW2X+4T+9xunvAx3xXzN2vTZ7PzYr6KV9TH7BNSG53L3x8QEqDOokE2uVNEOOs/gYFZTkavmNT5hkIZsdOj2C40bcnh1vJrZk9lf3G12B+MfVULl4RVw/XM/2Hcjrs66AeI=";
        String publicKey  = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzv0w/GsUWERxvjhOliBbhx+rcqL5qlQcNCeaJp76d0YitYgp86CezNdZivmtMQrIWbLdYLTEyPigzwtjmCTDo3JACkkmbTNBRJtkmqvWcglryWnctmLBUIGh7abCQNWBntfH+7MbJ+Wgn3bEGeWa9tTjs0g/8oUvYGyIA2EINJU7VvMP0OpHSYESyX2+ItNYCgrHgZNL/mEQ0m9xQii9I3FjL3UVckgklx9ggttLnWDToRUh6W327uMxpggAECoSlWpxSViWMaBN2CiCiGWdPwalyHWOMwUL5ymO9XBN+fyFw3KnZTy8Ori1c5h23uhElphanPCuFG3mz+gGbJb1QwIDAQAB";

        // ---- personalInfo（billing / shipping 共用相同内容）----
        Map<String, Object> personalInfo = new HashMap<>();
        personalInfo.put("firstName", "fist");
        personalInfo.put("lastName", "last");
        personalInfo.put("email", "demo@shopline.com");
        personalInfo.put("identityType", "xx");
        personalInfo.put("identityNumber", "xxx");
        personalInfo.put("gender", "1");
        personalInfo.put("phoneNumber", "xx");
        personalInfo.put("homeTelephone", "xx");
        personalInfo.put("birthDay", "1224");

        Map<String, Object> address = new HashMap<>();
        address.put("countryCode", "US");
        address.put("state", "California");
        address.put("stateCode", "US-CA");
        address.put("city", "demo city");
        address.put("district", "demo district");
        address.put("street", "demo street");
        address.put("street2", "demo street2");
        address.put("street3", "demo street3");
        address.put("postcode", "82371-2343");

        // ---- billing ----
        Map<String, Object> billing = new HashMap<>();
        billing.put("personalInfo", personalInfo);
        billing.put("address", address);

        // ---- shipping ----
        Map<String, Object> shipping = new HashMap<>();
        shipping.put("personalInfo", personalInfo);
        shipping.put("address", address);
        shipping.put("shippingMethod", "Standard shipping");
        shipping.put("carrier", "Standard shipping carrier");

        // ---- products[0] ----
        Map<String, Object> unitPrice = new HashMap<>();
        unitPrice.put("value", 1899);
        unitPrice.put("currency", "USD");

        Map<String, Object> product = new HashMap<>();
        product.put("id", "0_18073231123420620812342600");
        product.put("name", "product name");
        product.put("sku", "item-no");
        product.put("desc", "product desc");
        product.put("quantity", 1);
        product.put("unitPrice", unitPrice);

        // ---- amountBreakdown ----
        Map<String, Object> amountBreakdown = new HashMap<>();
        amountBreakdown.put("productAmount", 1899);
        amountBreakdown.put("discount", 0);
        amountBreakdown.put("productTax", 0);
        amountBreakdown.put("shippingAmount", 699);
        amountBreakdown.put("shippingTax", 0);
        amountBreakdown.put("other", 0);

        // ---- merchant ----
        Map<String, Object> merchant = new HashMap<>();
        merchant.put("storeWebsite", "https://abc.com");

        // ---- card ----
        Map<String, Object> card = new HashMap<>();
        card.put("cardNo", "4444444444444444");
        card.put("expirationMonth", "11");
        card.put("expirationYear", "24");
        card.put("cvv", "123");
        card.put("holderName", "test");

        // ---- client ----
        Map<String, Object> client = new HashMap<>();
        client.put("accept", "application/json");
        client.put("colorDepth", "24");
        client.put("ip", "xx.xx.xx.xx");
        client.put("javaEnabled", false);
        client.put("javaScriptEnabled", true);
        client.put("language", "en-US");
        client.put("screenHeight", "1920");
        client.put("screenWidth", "1080");
        client.put("timeZoneOffset", "-480");
        client.put("transactionWebSite", "xxx");
        client.put("userAgent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36");

        // ---- paymentOptions ----
        Map<String, Object> paymentOptions = new HashMap<>();
        paymentOptions.put("recurringPayment", true);
        paymentOptions.put("userBehavior", "bindPayment");
        paymentOptions.put("paymentInstrumentId", "abc");

        // ---- paymentMethodInstrument ----
        Map<String, Object> paymentMethodInstrument = new HashMap<>();
        paymentMethodInstrument.put("a", "b");

        //query
        Map<String, Object> paymentMethodInstrument1 = new HashMap<>();
        paymentMethodInstrument1.put("orderTransactionId", "2407354205016528273910-003");

        // ---- 顶层请求参数 ----
        Map<String, Object> params = new HashMap<>();
        params.put("orderTransactionId", "2407354205016528273910-004");
        params.put("referenceOrderId", "2407354205016528273910-004");
        params.put("amount", 101);
        params.put("currency", "INR");
        params.put("redirectUrl", "https://...");
        params.put("cancelUrl", "https://...");
        params.put("notifyUrl", "https://...");
        params.put("billing", billing);
        params.put("shipping", shipping);
        params.put("products", Collections.singletonList(product));
        params.put("amountBreakdown", amountBreakdown);
        params.put("merchant", merchant);
        params.put("card", card);
        params.put("client", client);
        params.put("paymentOptions", paymentOptions);
        params.put("paymentMethodInstrument", paymentMethodInstrument);


        Map<String, Object> params1 = new HashMap<>();
        params1.put("amount", "20000");
        params1.put("channel_order_transaction_id", "");
        params1.put("currency", "INR");
        params1.put("order_transaction_id", "2407583580432206970873-301");
        params1.put("status", "SUCCEEDED");




        String signSource = ShoplineSignUtil.buildSignatureSourceString(params1);
        System.out.println("=== 待签名文本 ===");
        System.out.println(signSource);
        System.out.println();

        String sign = ShoplineSignUtil.signPayRequest(privateKey, params);
        System.out.println("=== 签名值（pay-api-signature） ===");
        System.out.println(sign);



        boolean ok = ShoplineSignUtil.verifyPayCallback(publicKey, signSource, "E3AEsKui/0j9vYkTAwAiAeoO3RXQpXcYXw4vapihW4Gu+lGYnovgSg42eS10Foew4BUs8xmKpGXdmAGL/HmtcCu8idhqbkyOyH/sWGFV2amkxlHo8gKTZwTluWLruUD9zpHkzfB7p0Mr5ROO+GvCc/wpy8YpIkns6drXY4FbfMGUktv27Lond1IBcatYpPwtwiRvcVMlJX6VyOpYmiIpJNCVsC2INWpFwmXum0XiuU+5h6bkXHsG3dfmRB3lVSlD+DZAYmd4xQ4h7oj9etUcn0QwgX+ateTxZai92W8aRVyppqBO4NlYFqiPSvjJYVeOAiKuRLGgGxzvk6npBMflcg==");
        System.out.println("=== 自验签结果 ===");
        System.out.println(ok);


        String body = "{\"amount\":\"20000\",\"channel_order_transaction_id\":\"\",\"currency\":\"INR\",\"order_transaction_id\":\"2407572803907786409454-301\",\"status\":\"SUCCEEDED\"}";
        //String signature = request.getHeader("X-Shopline-Hmac-Sha256"); // 从请求头获取
        String appSecret = "e70f73b4ab5fc302e23e7ffbcb0af54cfdf0266f";

        System.out.println(ShoplineSignUtil.buildNotifySign(body,"e70f73b4ab5fc302e23e7ffbcb0af54cfdf0266f"));



        // 计算 HMAC-SHA256
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] bytes = sha256_HMAC.doFinal(body.getBytes(StandardCharsets.UTF_8));
        String expectedSign = ShoplineSignUtil.bytesToHex(bytes); // 转为十六进制字符串

        System.out.println(expectedSign);




    }
}
