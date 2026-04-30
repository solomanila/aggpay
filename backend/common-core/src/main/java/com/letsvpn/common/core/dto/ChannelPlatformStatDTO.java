package com.letsvpn.common.core.dto;

import lombok.Data;
import java.math.BigDecimal;

/** pay-service 按 (channel_id, platform_id) 分组的日统计数据 */
@Data
public class ChannelPlatformStatDTO {
    private String     statDate;      // yyyy-MM-dd
    private Long       channelId;
    private String     channelName;
    private Integer    platformId;    // 商户平台ID；无订单时为 null
    private BigDecimal systemAmount;  // 该商户在该通道当日成交额
}
