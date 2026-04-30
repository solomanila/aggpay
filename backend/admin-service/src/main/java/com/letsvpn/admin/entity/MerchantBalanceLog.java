package com.letsvpn.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("merchant_balance_log")
public class MerchantBalanceLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("platform_id")
    private Integer platformId;

    @TableField("currency")
    private String currency;

    /** RECHARGE / DEDUCT / FREEZE / UNFREEZE / WITHDRAW */
    @TableField("op_type")
    private String opType;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("before_available")
    private BigDecimal beforeAvailable;

    @TableField("after_available")
    private BigDecimal afterAvailable;

    @TableField("before_frozen")
    private BigDecimal beforeFrozen;

    @TableField("after_frozen")
    private BigDecimal afterFrozen;

    @TableField("remark")
    private String remark;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("created_at")
    private Date createdAt;
}
