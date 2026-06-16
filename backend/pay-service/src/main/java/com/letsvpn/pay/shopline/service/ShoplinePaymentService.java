package com.letsvpn.pay.shopline.service;

import cn.hutool.core.util.StrUtil;
import com.letsvpn.pay.constant.AreaTypeConstants;
import com.letsvpn.pay.entity.OrderInfo;
import com.letsvpn.pay.entity.PayPlatformInfo;
import com.letsvpn.pay.mapper.ext.ExtOrderInfoMapper;
import com.letsvpn.pay.mapper.ext.ExtPayPlatformInfoMapper;
import com.letsvpn.pay.service.core.PayReqService;
import com.letsvpn.pay.shopline.config.ShoplineConfig;
import com.letsvpn.pay.shopline.dto.ShoplinePayRequest;
import com.letsvpn.pay.shopline.dto.ShoplinePayResponse;
import com.letsvpn.pay.shopline.dto.ShoplineQueryResponse;
import com.letsvpn.pay.shopline.util.ShoplineSignUtil;
import com.letsvpn.pay.util.KeyValue;
import com.letsvpn.pay.util.PayCallMethod;
import com.letsvpn.pay.vo.PayResultData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShoplinePaymentService {

    private final ShoplineConfig shoplineConfig;
    private final ExtPayPlatformInfoMapper extPayPlatformInfoMapper;
    private final ExtOrderInfoMapper extOrderInfoMapper;
    private final PayReqService payReqService;

    private static final long UID = 31L;
    private static final String CID = "10";

    public ShoplinePayResponse pay(String storeHandle,
                                   ShoplinePayRequest req,
                                   String clientIp,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse,
                                   String extend3) {
        String returnMessageId = UUID.randomUUID().toString();

        // 1. 根据 storeHandle 查询 platformNo
        String platformNo = extPayPlatformInfoMapper.getPlatformNoByShopHandle(storeHandle);
        if (StrUtil.isBlank(platformNo)) {
            log.warn("shopline pay: no platformNo for handle={}", storeHandle);
            return fail(req.getOrderTransactionId(), returnMessageId, "店铺未绑定平台");
        }

        // 2. 加载平台信息（绕过签名验证，由上层 Controller 已完成 Shopline 签名校验）
        PayPlatformInfo platformInfo = extPayPlatformInfoMapper.getPayPlatformInfo(platformNo);
        if (platformInfo == null || platformInfo.getNullify() != 0) {
            log.warn("shopline pay: platform unavailable platformNo={}", platformNo);
            return fail(req.getOrderTransactionId(), returnMessageId, "平台不可用");
        }

        // 3. 构造内部下单参数
        long now = System.currentTimeMillis();
        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("fid", req.getOrderTransactionId());
        paramsMap.put("uid", String.valueOf(UID));
        paramsMap.put("amount", new java.math.BigDecimal(req.getAmount()).divide(java.math.BigDecimal.valueOf(100)).toPlainString());
        paramsMap.put("pf", platformNo);
        paramsMap.put("sign", "test");
        paramsMap.put("time", String.valueOf(now));
        paramsMap.put("cid", CID);

        log.info("shopline pay: calling payReqService handle={} orderTransactionId={} amount={}",
                storeHandle, req.getOrderTransactionId(), req.getAmount());

        // 4. 调用内部下单服务
        KeyValue<PayCallMethod, PayResultData> result;
        try {
            result = payReqService.req(
                    paramsMap, platformInfo, clientIp,
                    new StringBuffer(httpRequest.getRequestURL()),
                    httpRequest.getRequestURI(), "",
                    httpResponse, UID);
        } catch (Exception e) {
            log.error("shopline pay: payReqService failed handle={} err={}", storeHandle, e.getMessage(), e);
            return fail(req.getOrderTransactionId(), returnMessageId, e.getMessage());
        }

        if (result == null) {
            return fail(req.getOrderTransactionId(), returnMessageId, "下单无结果");
        }

        // 5. 解析结果：只处理 sdk 方法（返回支付链接）
        PayResultData data = result.getValue();
        String paymentUrl = data != null ? data.getLink() : null;
        String orderId = data != null ? data.getOrderId() : null;

        if (StrUtil.isBlank(paymentUrl)) {
            log.warn("shopline pay: empty paymentUrl handle={}", storeHandle);
            return fail(req.getOrderTransactionId(), returnMessageId, "未获得支付地址");
        }

        // 6. 将 Shopline 回调信息写入 extend3
        if (StrUtil.isNotBlank(orderId) && StrUtil.isNotBlank(extend3)) {
            try {
                extOrderInfoMapper.updateExtend3ByOrderId(orderId, extend3);
                log.info("shopline pay: extend3 saved orderId={}", orderId);
            } catch (Exception e) {
                log.warn("shopline pay: extend3 save failed orderId={} err={}", orderId, e.getMessage());
            }
        }

        // 7. 构造 SUCCESS 响应
        ShoplinePayResponse resp = new ShoplinePayResponse();
        resp.setReturnCode("SUCCESS");
        resp.setReturnMessage("");
        resp.setReturnMessageId(returnMessageId);
        resp.setOrderTransactionId(req.getOrderTransactionId());
        resp.setChannelOrderTransactionId(orderId);
        resp.setPaymentUrl(paymentUrl);

        log.info("shopline pay: success handle={} orderId={} paymentUrl={}",
                storeHandle, orderId, paymentUrl);
        return resp;
    }

    private ShoplinePayResponse fail(String orderTransactionId, String returnMessageId, String message) {
        ShoplinePayResponse resp = new ShoplinePayResponse();
        resp.setReturnCode("FAIL");
        resp.setReturnMessage(message);
        resp.setReturnMessageId(returnMessageId);
        resp.setOrderTransactionId(orderTransactionId);
        resp.setChannelOrderTransactionId("");
        return resp;
    }

    public ShoplineQueryResponse query(String orderTransactionId) {
        String returnMessageId = UUID.randomUUID().toString();

        OrderInfo order = extOrderInfoMapper.getOrderInfoByFrontIdOnly(orderTransactionId);
        if (order == null) {
            log.warn("shopline query: order not found orderTransactionId={}", orderTransactionId);
            ShoplineQueryResponse resp = new ShoplineQueryResponse();
            resp.setReturnCode("FAIL");
            resp.setReturnMessage("订单不存在");
            resp.setReturnMessageId(returnMessageId);
            resp.setOrderTransactionId(orderTransactionId);
            resp.setChannelOrderTransactionId("");
            return resp;
        }

        String paymentStatus = mapOrderStatus(order.getStatus());

        String currency = "";
        if (order.getPlatformId() != null) {
            PayPlatformInfo platformInfo = extPayPlatformInfoMapper.getPayPlatformInfoId(order.getPlatformId());
            if (platformInfo != null) {
                currency = StrUtil.nullToEmpty(AreaTypeConstants.currencyCode(platformInfo.getAreaType()));
            }
        }

        log.info("shopline query: orderTransactionId={} orderId={} status={} paymentStatus={} currency={}",
                orderTransactionId, order.getOrderId(), order.getStatus(), paymentStatus, currency);

        ShoplineQueryResponse resp = new ShoplineQueryResponse();
        resp.setReturnCode("SUCCESS");
        resp.setReturnMessage("");
        resp.setReturnMessageId(returnMessageId);
        resp.setOrderTransactionId(orderTransactionId);
        resp.setChannelOrderTransactionId(order.getOrderId());
        resp.setPaymentStatus(paymentStatus);
        resp.setAmount(order.getReqAmount() != null ? order.getReqAmount().longValue() : 0L);
        resp.setCurrency(currency);
        if ("FAILED".equals(paymentStatus)) {
            resp.setFailCode("PAY_FAIL");
            resp.setFailMessage(StrUtil.isNotBlank(order.getRemark()) ? order.getRemark() : "Payment failed");
        }
        return resp;
    }

    private String mapOrderStatus(Integer status) {
        if (status == null) return "PROCESSING";
        switch (status) {
            case 1: return "SUCCEEDED";
            case 2: return "FAILED";
            case 3: return "CANCELLED";
            default: return "PROCESSING";
        }
    }
}
