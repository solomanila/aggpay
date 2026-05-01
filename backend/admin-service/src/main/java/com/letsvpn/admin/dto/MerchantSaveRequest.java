package com.letsvpn.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商户创建/更新请求（基本信息 + 运营配置合并）
 */
@Data
public class MerchantSaveRequest {
    /** 更新时必填，创建时为 null */
    private Integer platformId;

    // ── pay_platform_info ──────────────────────────────────────────
    private String title;
    /** 前端语义: 1=启用, 0=禁用 (服务层转换为 nullify: 0=启用, 1=禁用) */
    private Integer status;

    // ── merchant_op_config ─────────────────────────────────────────
    private Long agentId;
    private String email;
    private String remark;
    private Integer dailyPayOrderLimit;
    private Integer dailyWithdrawCountLimit;
    private BigDecimal dailyWithdrawAmountLimit;
    private BigDecimal dailyPayLimit;
    private BigDecimal dailyPayoutLimit;
    private Integer largePayoutRiskEnabled;
    private BigDecimal largePayoutRiskAmount;
    private String telegramGroupId;
    private Integer settlementNotify;

    // system_user_auth 账号信息（创建时必填）
    private String account;
    private String password;
}
