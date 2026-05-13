package com.letsvpn.admin.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ChannelProfileVO {
    private Long id;
    private String name;
    private List<String> businessTypes;
    private String areaType;
    private BigDecimal feeRate;
    private BigDecimal costRate;
    private Map<String, Object> extra;
}
