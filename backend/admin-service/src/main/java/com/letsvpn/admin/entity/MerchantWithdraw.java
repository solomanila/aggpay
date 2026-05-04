package com.letsvpn.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商户提现申请表
 * status: 0=审核中 1=已通过 2=已拒绝
 */
@Data
@TableName("merchant_withdraw")
public class MerchantWithdraw {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("platform_id")
    private Integer platformId;

    @TableField("currency")
    private String currency;

    @TableField("status")
    private Integer status;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("usdt_address")
    private String usdtAddress;

    @TableField("create_time")
    private Date createTime;
}
