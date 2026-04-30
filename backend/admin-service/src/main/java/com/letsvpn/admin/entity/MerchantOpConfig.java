package com.letsvpn.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("merchant_op_config")
public class MerchantOpConfig {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("platform_id")
    private Integer platformId;

    @TableField("email")
    private String email;

    @TableField("agent_id")
    private Long agentId;

    @TableField("remark")
    private String remark;

    @TableField("daily_pay_order_limit")
    private Integer dailyPayOrderLimit;

    @TableField("daily_withdraw_count_limit")
    private Integer dailyWithdrawCountLimit;

    @TableField("daily_withdraw_amount_limit")
    private BigDecimal dailyWithdrawAmountLimit;

    @TableField("daily_pay_limit")
    private BigDecimal dailyPayLimit;

    @TableField("daily_payout_limit")
    private BigDecimal dailyPayoutLimit;

    @TableField("large_payout_risk_enabled")
    private Integer largePayoutRiskEnabled;

    @TableField("large_payout_risk_amount")
    private BigDecimal largePayoutRiskAmount;

    @TableField("telegram_group_id")
    private String telegramGroupId;

    @TableField("settlement_notify")
    private Integer settlementNotify;

    @TableField("created_at")
    private Date createdAt;

    @TableField("updated_at")
    private Date updatedAt;
}
