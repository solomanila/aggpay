package com.letsvpn.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

@Data
@TableName("pay_channel_profile")
public class PayChannelProfile {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("channel_code")
    private String channelCode;

    @TableField("name")
    private String name;

    @TableField("area_type")
    private String areaType;

    @TableField("status")
    private String status;

    @TableField("entity_id")
    private Long entityId;

    @TableField("fee_rate")
    private java.math.BigDecimal feeRate;

    @TableField("cost_rate")
    private java.math.BigDecimal costRate;

    @TableField("routing_weight")
    private Integer routingWeight;

    @TableField("limit_config")
    private String limitConfig;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("updated_at")
    private Date updatedAt;
}
