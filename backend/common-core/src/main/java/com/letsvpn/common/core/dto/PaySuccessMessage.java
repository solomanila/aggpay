package com.letsvpn.common.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    /** 消息发送时间戳（毫秒） */
    private Long timestamp;
}
