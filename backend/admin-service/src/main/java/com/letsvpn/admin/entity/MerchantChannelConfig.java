package com.letsvpn.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("merchant_channel_config")
public class MerchantChannelConfig {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("platform_id")
    private Integer platformId;

    @TableField("pay_config_channel_id")
    private Long payConfigChannelId;

    /** PAYMENT / PAYOUT */
    @TableField("channel_type")
    private String channelType;

    @TableField("daily_limit")
    private BigDecimal dailyLimit;

    @TableField("weight")
    private Integer weight;

    @TableField("enabled")
    private Integer enabled;

    @TableField("start_time")
    private String startTime;

    @TableField("end_time")
    private String endTime;

    @TableField("settlement_cycle")
    private Integer settlementCycle;

    @TableField("auto_settle")
    private Integer autoSettle;

    @TableField("min_amount")
    private BigDecimal minAmount;

    @TableField("max_amount")
    private BigDecimal maxAmount;

    @TableField("currency")
    private String currency;

    @TableField("pay_mode")
    private String payMode;

    @TableField("pay_page")
    private String payPage;

    @TableField("fee_rate")
    private BigDecimal feeRate;

    @TableField("fee_fixed")
    private BigDecimal feeFixed;

    @TableField("tiered_rate_enabled")
    private Integer tieredRateEnabled;

    @TableField("tiered_rate_config")
    private String tieredRateConfig;

    @TableField("created_at")
    private Date createdAt;

    @TableField("updated_at")
    private Date updatedAt;
}
