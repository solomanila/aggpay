package com.letsvpn.admin.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.letsvpn.admin.constant.AreaTypeConstants;
import com.letsvpn.admin.entity.SystemUserAuth;
import com.letsvpn.admin.mapper.SystemUserAuthMapper;
import com.letsvpn.admin.service.MerchantBalanceService;
import com.letsvpn.admin.service.PayServiceFacade;
import com.letsvpn.common.core.dto.MerchantProfileDTO;
import com.letsvpn.common.core.dto.OrderInfoDTO;
import com.letsvpn.common.core.dto.PaySuccessMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = "pay_order_success",
        selectorExpression = "PAY_SUCCESS",
        consumerGroup = "admin-credit-consumer-group"
)
public class CreditOrderIncomeConsumer implements RocketMQListener<PaySuccessMessage> {

    @Autowired
    private PayServiceFacade payServiceFacade;
    @Autowired
    private MerchantBalanceService merchantBalanceService;
    @Autowired
    private SystemUserAuthMapper systemUserAuthMapper;

    @Override
    public void onMessage(PaySuccessMessage message) {
        if (!Integer.valueOf(1).equals(message.getStatus())) return;

        OrderInfoDTO orderInfo = payServiceFacade.fetchOrderInfoById(message.getOrderId());
        if (orderInfo == null || orderInfo.getPlatformId() == null) {
            log.warn("creditOrderIncome: orderInfo not found or no platformId, orderId={}", message.getOrderId());
            return;
        }

        SystemUserAuth auth = systemUserAuthMapper.selectOne(new LambdaQueryWrapper<SystemUserAuth>()
                .eq(SystemUserAuth::getPlatformId, orderInfo.getPlatformId())
                .isNotNull(SystemUserAuth::getPlatformId)
                .last("LIMIT 1"));
        if (auth == null) return;

        MerchantProfileDTO profile = payServiceFacade.fetchMerchantDetail(orderInfo.getPlatformId());
        String currencyCode = profile != null && profile.getAreaType() != null
                ? AreaTypeConstants.currencyCode(profile.getAreaType())
                : null;
        if (currencyCode == null) {
            log.warn("creditOrderIncome: unknown areaType for platformId={}, orderId={}",
                    orderInfo.getPlatformId(), message.getOrderId());
            return;
        }

        merchantBalanceService.creditOrderIncome(orderInfo, currencyCode);
        log.info("creditOrderIncome: settled orderId={} platformId={} amount={} {}",
                message.getOrderId(), orderInfo.getPlatformId(), orderInfo.getRealAmount(), currencyCode);
    }
}
