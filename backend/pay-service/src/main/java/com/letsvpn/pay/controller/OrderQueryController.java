package com.letsvpn.pay.controller;

import cn.hutool.core.map.MapUtil;
import com.letsvpn.common.core.dto.MerchantOrderQueryDTO;
import com.letsvpn.pay.entity.OrderInfo;
import com.letsvpn.pay.entity.PayPlatformInfo;
import com.letsvpn.pay.service.base.IOrderInfoService;
import com.letsvpn.pay.service.core.PayRateLimitService;
import com.letsvpn.pay.util.PayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 对外商户查单接口。
 * 安全层级：HMAC签名 + 平台鉴权 + 时间防重放 + IP限速 + 商户归属校验 + 字段脱敏
 */
@RestController
@RequestMapping("/pay/order")
public class OrderQueryController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(OrderQueryController.class);

    @Autowired
    private IOrderInfoService orderInfoService;

    @Autowired
    private PayRateLimitService payRateLimitService;

    /**
     * 按 orderId 查询订单（商户只能查自己平台的订单）。
     *
     * 请求参数：
     *   pf        平台编号
     *   orderId   平台内订单号
     *   time      时间戳（毫秒），用于防重放
     *   sign      HMAC 签名（所有参数按 key 升序拼接后用 secretKey 签名）
     */
    @GetMapping("/query")
    public MerchantOrderQueryDTO query(@RequestParam Map<String, String> paramsMap,
                                       HttpServletRequest request) {
        String ip = getIp(request);
        payRateLimitService.checkIpLimit(ip);

        // 签名验证 + 平台鉴权（含时间防重放）/**/
        PayPlatformInfo platform = validateSign(paramsMap);

        String orderId = MapUtil.getStr(paramsMap, "orderId");
        PayUtil.validate(orderId == null || orderId.isBlank(), "orderId 不能为空", 1041);
        PayUtil.validate(orderId.length() > 64, "orderId 长度超限", 1042);

        logger.info("merchant order query: pf={} orderId={} ip={}", platform.getPlatformNo(), orderId, ip);

        OrderInfo order = orderInfoService.getByOrderId(orderId);
        PayUtil.validate(order == null, "订单不存在", 1040);

        // 商户只能查属于自己平台的订单
        PayUtil.validate(!platform.getPlatformId().equals(order.getPlatformId()), "无权查询该订单", 1043);

        return toMerchantDTO(order);
    }

    private MerchantOrderQueryDTO toMerchantDTO(OrderInfo o) {
        MerchantOrderQueryDTO dto = new MerchantOrderQueryDTO();
        dto.setOrderId(o.getOrderId());
        dto.setFrontId(o.getFrontId());
        dto.setPlatformId(o.getPlatformId());
        dto.setUserId(o.getUserId());
        dto.setGameId(o.getGameId());
        dto.setStatus(o.getStatus());
        dto.setReqAmount(o.getReqAmount());
        dto.setRealAmount(o.getRealAmount());
        dto.setPayTime(o.getPayTime());
        dto.setCreateTime(o.getCreateTime());
        dto.setNoticeStatus(o.getNoticeStatus());
        dto.setRemark(o.getRemark());
        return dto;
    }
}
