package com.letsvpn.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("merchant_balance")
public class MerchantBalance {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("platform_id")
    private Integer platformId;

    @TableField("currency")
    private String currency;

    @TableField("available")
    private BigDecimal available;

    @TableField("frozen")
    private BigDecimal frozen;

    @TableField("updated_at")
    private Date updatedAt;
}
