package com.letsvpn.common.core.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 简单的首页指标返回对象，方便各服务之间复用。
 */
@Data
public class HomeDashboardMetricsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 运营国家（area_type）去重集合
     */
    private List<Integer> operatingCountries;

    /**
     * 活跃通道数量
     */
    private Long activeChannelCount;

    /**
     * 分钟级 SLA，暂时固定为 99.95%
     */
    private String minuteLevelSla;
}
