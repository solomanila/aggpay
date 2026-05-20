package com.letsvpn.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("bill")
public class Bill {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("account")
    private String account;

    @TableField("platform_id")
    private Long platformId;

    @TableField("channel_title")
    private String channelTitle;

    @TableField("pay_config_channel_id")
    private Long payConfigChannelId;

    @TableField("amount")
    private BigDecimal amount;

    /** 1=Success 0=Fail */
    @TableField("status")
    private Integer status;

    @TableField("settle_at")
    private Date settleAt;

    @TableField("created_at")
    private Date createdAt;

    @TableField("updated_at")
    private Date updatedAt;
}
