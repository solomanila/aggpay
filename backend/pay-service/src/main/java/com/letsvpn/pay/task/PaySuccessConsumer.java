package com.letsvpn.pay.task;

import com.letsvpn.common.core.dto.PaySuccessMessage;
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
 *
 * <p>分布式部署说明：
 * <ul>
 *   <li>同一 consumerGroup 在集群模式下，每条消息只投递给一个实例（RocketMQ 负载均衡）。</li>
 *   <li>onMessage 同步处理：方法返回才 ACK。业务异常会触发 RocketMQ 重试（默认最多16次），
 *       超限后进入死信队列（DLQ: %DLQ%pay-success-consumer-group）。</li>
 *   <li>幂等保障：pushSuccessOrderInfo 内部以 noticeStatus=100 为幂等标记，重复投递安全。</li>
 * </ul>
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
        // 同步执行，不捕获异常：失败时 RocketMQ 将重试此消息
        payPushPanService.pushSuccessOrderInfo(message.getOrderId());
    }
}
