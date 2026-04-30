package com.letsvpn.pay.task;

import com.letsvpn.pay.dto.PaySuccessMessage;
import com.letsvpn.pay.service.core.PayPushPanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 支付成功/退款状态变更 MQ 消费者
 *
 * <p>消费 Topic: pay_order_success，Tag: PAY_SUCCESS || REFUND_UPDATE
 * <p>幂等保障：PayPushPanService._pushSuccessOrderInfo 内部通过 noticeStatus==100 判断，重复消费安全。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "pay_order_success",
        selectorExpression = "PAY_SUCCESS || REFUND_UPDATE",
        consumerGroup = "pay-success-consumer-group"
)
public class PaySuccessConsumer implements RocketMQListener<PaySuccessMessage> {

    @Autowired
    private PayPushPanService payPushPanService;

    @Override
    public void onMessage(PaySuccessMessage message) {
        log.info("MQ consume pay_order_success: orderId={}, orderNo={}, status={}",
                message.getOrderId(), message.getOrderNo(), message.getStatus());
        payPushPanService.pushSuccessOrderInfo(message.getOrderId());
    }
}
