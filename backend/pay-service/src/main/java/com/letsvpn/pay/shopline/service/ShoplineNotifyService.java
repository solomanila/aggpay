package com.letsvpn.pay.shopline.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.letsvpn.common.core.dto.PaySuccessMessage;
import com.letsvpn.pay.entity.OrderInfo;
import com.letsvpn.pay.entity.PayPlatformInfo;
import com.letsvpn.pay.mapper.OrderInfoMapper;
import com.letsvpn.pay.mapper.ext.ExtPayPlatformInfoMapper;
import com.letsvpn.pay.shopline.config.ShoplineConfig;
import com.letsvpn.pay.shopline.entity.ShoplineShopToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

/**
 * 监听支付成功 MQ 消息，向 Shopline 通知支付结果。
 *
 * 触发条件：订单的 platform_no 在 shopline_shop_token.shop_id 中存在记录（即该平台已完成 Shopline OAuth 授权）
 * Shopline checkout token 存在 order_info.extend1
 */
@Service
@RocketMQMessageListener(
        topic = "pay_order_success",
        selectorExpression = "PAY_SUCCESS || REFUND_UPDATE",
        consumerGroup = "shopline-payment-notify-group"
)
@RequiredArgsConstructor
@Slf4j
public class ShoplineNotifyService implements RocketMQListener<PaySuccessMessage> {

    private final ShoplineConfig shoplineConfig;
    private final ShoplineOAuthService shoplineOAuthService;
    private final OrderInfoMapper orderInfoMapper;
    private final ExtPayPlatformInfoMapper extPayPlatformInfoMapper;

    @Override
    public void onMessage(PaySuccessMessage message) {
        Long orderId = message.getOrderId();
        Integer status = message.getStatus();
        log.info("ShoplineNotifyService received: orderId={}, status={}", orderId, status);

        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            log.warn("Order not found: id={}", orderId);
            return;
        }

        if (order.getPlatformId() == null) {
            return;
        }

        // 通过 platformId 查 platform_no，再查 shopline_shop_token 确认是否属于 Shopline 平台
        PayPlatformInfo platform = extPayPlatformInfoMapper.getPayPlatformInfoId(order.getPlatformId());
        if (platform == null) {
            return;
        }

        ShoplineShopToken shopToken = shoplineOAuthService.getByShopId(platform.getPlatformNo());
        if (shopToken == null || StrUtil.isBlank(shopToken.getAccessToken())) {
            return;
        }

        String checkoutToken = order.getExtend1();  // Shopline checkout session token

        if (StrUtil.isBlank(checkoutToken)) {
            log.warn("Shopline order missing extend1 (checkoutToken): orderId={}", orderId);
            return;
        }

        if (status == 1) {
            resolvePaymentSession(shopToken, checkoutToken, order);
        } else if (status == 8) {
            log.info("Refund complete for Shopline order: orderId={}, checkoutToken={}", orderId, checkoutToken);
        }
    }

    /**
     * 调用 Shopline payment session resolve 接口，通知支付成功。
     *
     * 接口文档: https://developer.myshopline.com/docsv2/ec20/... (payment_sessions resolve)
     * 请根据实际 Shopline API 规范调整 URL 和请求体格式。
     */
    private void resolvePaymentSession(ShoplineShopToken shopToken, String checkoutToken, OrderInfo order) {
        String url = shoplineConfig.getApiBase()
                + "/openapi/" + shoplineConfig.getApiVersion()
                + "/payment_sessions/" + checkoutToken + "/resolve";

        JSONObject body = new JSONObject();
        body.put("payment_status", "success");
        body.put("amount", order.getRealAmount() != null ? order.getRealAmount().toPlainString() : "");

        log.info("Shopline resolve payment: url={}, shopId={}, checkoutToken={}",
                url, shopToken.getShopId(), checkoutToken);
        try {
            String result = HttpRequest.post(url)
                    .header("X-Shopline-Access-Token", shopToken.getAccessToken())
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(10000)
                    .executeAsync()
                    .body();
            log.info("Shopline resolve response: checkoutToken={}, result={}", checkoutToken, result);
        } catch (Exception e) {
            log.error("Shopline resolve failed: checkoutToken={}, err={}", checkoutToken, e.getMessage(), e);
        }
    }
}
