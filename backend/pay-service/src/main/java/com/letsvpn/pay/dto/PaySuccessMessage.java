package com.letsvpn.pay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RocketMQ 支付成功/退款状态变更消息体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaySuccessMessage {

    /** 数据库主键 id (order_info.id) */
    private Long orderId;

    /** 业务订单号 (order_info.order_id) */
    private String orderNo;

    /**
     * 订单状态：
     * 1=支付成功, 6=待退款, 7=退款中, 8=退款成功, 9=退款失败
     */
    private Integer status;

    /** 消息发送时间戳（毫秒），用于辅助日志排查 */
    private Long timestamp;
}
