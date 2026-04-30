package com.letsvpn.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

@Data
@TableName("pay_channel_limit_plan")
public class PayChannelLimitPlan {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("channel_id")
    private String channelId;

    @TableField("plan_type")
    private String planType;

    @TableField("window_minutes")
    private Integer windowMinutes;

    @TableField(exist = false)
    private Integer limitCount;

    @TableField("status")
    private String status;

    @TableField(exist = false)
    private Date createTime;
}
