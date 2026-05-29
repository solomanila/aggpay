package com.letsvpn.pay.shopline.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.letsvpn.common.core.dto.PaySuccessMessage;
import com.letsvpn.pay.entity.OrderInfo;
import com.letsvpn.pay.mapper.OrderInfoMapper;
import com.letsvpn.pay.mapper.ext.ExtPayPlatformInfoMapper;
import com.letsvpn.pay.shopline.config.ShoplineConfig;
import com.letsvpn.pay.shopline.entity.ShoplineShopToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * 监听支付成功 MQ 消息，向 Shopline 通知支付结果。
 *
 * 触发条件：订单属于 Shopline 平台（platform_no = shopline.platform-no 配置）
 * Shopline checkout token 存在 order_info.extend1
 * 对应商家的 access_token 存在 order_info.extend2（shop_id）→ shopline_shop_token
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

    private Integer shoplinePlatformId;

    @PostConstruct
    public void init() {
        try {
            var platform = extPayPlatformInfoMapper.getPayPlatformInfo(shoplineConfig.getPlatformNo());
            if (platform != null) {
                shoplinePlatformId = platform.getPlatformId();
                log.info("Shopline platformId resolved: platformNo={}, platformId={}",
                        shoplineConfig.getPlatformNo(), shoplinePlatformId);
            } else {
                log.warn("Shopline platform not found for platformNo={}", shoplineConfig.getPlatformNo());
            }
        } catch (Exception e) {
            log.error("Failed to resolve Shopline platformId: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onMessage(PaySuccessMessage message) {
        if (shoplinePlatformId == null) {
            return;
        }

        Long orderId = message.getOrderId();
        Integer status = message.getStatus();
        log.info("ShoplineNotifyService received: orderId={}, status={}", orderId, status);

        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            log.warn("Order not found: id={}", orderId);
            return;
        }

        // 只处理属于 Shopline 平台的订单
        if (!shoplinePlatformId.equals(order.getPlatformId())) {
            return;
        }

        String checkoutToken = order.getExtend1();  // Shopline checkout session token
        String shopId = order.getExtend2();          // Shopline shop_id

        if (StrUtil.isBlank(checkoutToken) || StrUtil.isBlank(shopId)) {
            log.warn("Shopline order missing extend1/extend2: orderId={}", orderId);
            return;
        }

        ShoplineShopToken shopToken = shoplineOAuthService.getByShopId(shopId);
        if (shopToken == null || StrUtil.isBlank(shopToken.getAccessToken())) {
            log.error("No access_token for shopId={}, cannot notify Shopline", shopId);
            return;
        }

        if (status == 1) {
            resolvePaymentSession(shopToken, checkoutToken, order);
        } else if (status == 8) {
            log.info("Refund complete for Shopline order: orderId={}, checkoutToken={}", orderId, checkoutToken);
            // 退款通知逻辑（如需向 Shopline 通知退款结果，在此实现）
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
