package com.letsvpn.common.core.dto;

import lombok.Data;

@Data
public class PayConfigChannelUpdateRequest {

    /** pay_config_info.id (required) */
    private Integer id;

    /** pay_config_info.title */
    private String title;

    /** pay_config_info.area_type */
    private Integer areaType;

    /** pay_config_info.remark */
    private String remark;

    /** pay_config_info.nullify (0=启用, 1=禁用) */
    private Integer nullify;

    /** pay_config_info.third_service */
    private String thirdService;

    /** pay_config_info.short_code */
    private String shortCode;

    /**
     * pay_config_info.req_param — JSON string storing extra params:
     * channelType, settlementDays, settlementStatus, minAmount, maxAmount,
     * dailyLimit, pendingDailyLimit, secret, provider, mode,
     * submitUtrForwardChatId, webhook, riskControl
     */
    private String reqParam;

    /** merchant_info.app_id */
    private String appId;

    /** merchant_info.private_key */
    private String privateKey;
}
