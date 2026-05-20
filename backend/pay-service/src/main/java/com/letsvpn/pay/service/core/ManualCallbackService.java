package com.letsvpn.pay.service.core;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.letsvpn.common.core.dto.PaySuccessMessage;
import com.letsvpn.pay.entity.OrderInfo;
import com.letsvpn.pay.mapper.OrderInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 手动回调：模拟三方回调成功，触发与真实回调完全相同的后续流程。
 *
 * 复现 PayNotifyService.endStep() 的支付成功分支逻辑：
 *   1. 校验订单状态
 *   2. 更新 order_info（status/pay_time/real_amount/settle_amount/sync_status）
 *   3. 发送 MQ pay_order_success:PAY_SUCCESS
 *      → PaySuccessConsumer → creditOrderIncome + noticeStatus=100
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManualCallbackService {

    private final OrderInfoMapper  orderInfoMapper;
    private final RocketMQTemplate rocketMQTemplate;

    public void manualCallback(String orderId) {
        // 1. 查询订单
        OrderInfo order = orderInfoMapper.selectOne(
                Wrappers.<OrderInfo>lambdaQuery()
                        .eq(OrderInfo::getOrderId, orderId));
        if (order == null) {
            throw new IllegalArgumentException("订单不存在: " + orderId);
        }

        // 2. 状态校验（与 endStep() 保持一致）
        if (Integer.valueOf(1).equals(order.getStatus())) {
            throw new IllegalStateException("订单已支付成功，无需重复处理: " + orderId);
        }
        if (Integer.valueOf(2).equals(order.getStatus())) {
            throw new IllegalStateException("订单已挂起，不允许手动回调: " + orderId);
        }
        if (Integer.valueOf(8).equals(order.getStatus())) {
            throw new IllegalStateException("订单已退款完成，不允许手动回调: " + orderId);
        }

        // 3. 更新 order_info（与 endStep() PAY_SUCCESS 分支字段完全对齐）
        //    手动回调无上游金额，使用 req_amount 作为 real_amount
        OrderInfo record = new OrderInfo();
        record.setId(order.getId());
        record.setStatus(1);
        record.setPayTime(new Date());
        record.setRealAmount(order.getReqAmount());
        record.setSettleAmount(order.getReqAmount());
        record.setSyncStatus(1);

        int updated = orderInfoMapper.updateById(record);
        if (updated <= 0) {
            throw new IllegalStateException("order_info 更新失败: " + orderId);
        }
        log.info("ManualCallback: order updated to status=1, orderId={}, id={}", orderId, order.getId());

        // 4. 发送 MQ → 触发 PaySuccessConsumer（余额入账 + noticeStatus=100）
        //    MQ 发送失败不影响订单状态，记录日志等待人工处理或重试
        try {
            rocketMQTemplate.convertAndSend(
                    "pay_order_success:PAY_SUCCESS",
                    PaySuccessMessage.builder()
                            .orderId(order.getId())
                            .orderNo(orderId)
                            .status(1)
                            .timestamp(System.currentTimeMillis())
                            .build()
            );
            log.info("ManualCallback: MQ sent PAY_SUCCESS, orderId={}, id={}", orderId, order.getId());
        } catch (Exception e) {
            log.error("ManualCallback: MQ send failed, orderId={}, id={}, err={}",
                    orderId, order.getId(), e.getMessage(), e);
        }
    }
}
