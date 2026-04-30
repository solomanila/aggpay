package com.letsvpn.common.core.dto;

import lombok.Data;
import java.math.BigDecimal;

/** pay-service 定时任务通过 Feign 向 admin-service 批量提交的通道日统计数据 */
@Data
public class ChannelProfitStatDTO {
    private String statDate;       // yyyy-MM-dd
    private Long channelId;
    private String channelName;
    private String channelType;    // PAYMENT / PAYOUT
    private BigDecimal systemAmount;
}
