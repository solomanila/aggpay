package com.letsvpn.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("channel_profit_stat")
public class ChannelProfitStat {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("stat_date")
    private String statDate;

    @TableField("channel_id")
    private Long channelId;

    @TableField("channel_name")
    private String channelName;

    @TableField("system_amount")
    private BigDecimal systemAmount;

    @TableField("channel_fee_income")
    private BigDecimal channelFeeIncome;

    @TableField("collection_amount")
    private BigDecimal collectionAmount;

    @TableField("dropped_order_income")
    private BigDecimal droppedOrderIncome;

    @TableField("channel_cost")
    private BigDecimal channelCost;

    @TableField("other_cost")
    private BigDecimal otherCost;

    @TableField("frozen_amount")
    private BigDecimal frozenAmount;

    @TableField("adjustment")
    private BigDecimal adjustment;

    @TableField("profit")
    private BigDecimal profit;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
