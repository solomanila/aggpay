package com.letsvpn.pay.shopline.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.letsvpn.pay.shopline.config.ShoplineConfig;
import com.letsvpn.pay.shopline.service.ShoplineOAuthService;
import com.letsvpn.pay.shopline.util.ShoplineSignUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pay/shopline/webhook")
@RequiredArgsConstructor
@Slf4j
public class ShoplineWebhookController {

    private final ShoplineConfig shoplineConfig;
    private final ShoplineOAuthService shoplineOAuthService;

    /**
     * Shopline Webhook 统一入口。
     * 外部路径（经网关）：POST /api/pay/shopline/webhook
     * 通过 X-Shopline-Topic 区分事件类型：
     *   - apps/installed_uninstalled：应用安装/卸载状态变更
     *   - merchants/redact：GDPR 商户数据删除
     */
    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("X-Shopline-Topic") String topic,
            @RequestHeader("X-Shopline-Hmac-Sha256") String signature,
            @RequestHeader(value = "X-Shopline-Shop-Domain", required = false) String shopDomain,
            @RequestHeader(value = "X-Shopline-Webhook-Id", defaultValue = "") String webhookId,
            @RequestBody String rawBody) {

        log.info("Shopline webhook: topic={}, webhookId={}, shopDomain={}", topic, webhookId, shopDomain);

        if (!ShoplineSignUtil.verifyWebhookHmac(rawBody, signature, shoplineConfig.getAppSecret())) {
            log.warn("Shopline webhook HMAC failed: topic={}, webhookId={}", topic, webhookId);
            // 仍返回 200，避免 Shopline 因验签失败反复重试
            return ResponseEntity.ok().build();
        }

        try {
            switch (topic) {
                case "apps/installed_uninstalled":
                    handleAppStatusChange(rawBody, webhookId);
                    break;
                case "merchants/redact":
                    handleMerchantsRedact(rawBody, webhookId);
                    break;
                default:
                    log.info("Shopline webhook unhandled topic: {}", topic);
            }
        } catch (Exception e) {
            log.error("Shopline webhook error: topic={}, webhookId={}, err={}", topic, webhookId, e.getMessage(), e);
        }

        return ResponseEntity.ok().build();
    }

    private void handleAppStatusChange(String rawBody, String webhookId) {
        JSONObject body = JSONUtil.parseObj(rawBody);
        String operate = body.getStr("operate");
        String handle  = body.getStr("handle");

        log.info("Shopline app status: operate={}, handle={}, webhookId={}", operate, handle, webhookId);

        if ("UNINSTALL".equals(operate) && handle != null) {
            int rows = shoplineOAuthService.deleteByHandle(handle);
            log.info("Shopline uninstall: deleted {} token(s), handle={}", rows, handle);
        }
    }

    private void handleMerchantsRedact(String rawBody, String webhookId) {
        JSONObject body      = JSONUtil.parseObj(rawBody);
        String storeDomain   = body.getStr("store_domain");
        String storeId       = body.getStr("store_id");

        log.info("Shopline merchants/redact: storeDomain={}, storeId={}, webhookId={}", storeDomain, storeId, webhookId);

        if (storeDomain != null) {
            // store_domain 格式为 {handle}.myshopline.com
            String handle = storeDomain.contains(".") ? storeDomain.split("\\.")[0] : storeDomain;
            int rows = shoplineOAuthService.deleteByHandle(handle);
            log.info("Shopline redact: deleted {} token(s), handle={}", rows, handle);
        }
    }
}
