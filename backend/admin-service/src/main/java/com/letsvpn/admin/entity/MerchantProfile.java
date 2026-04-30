package com.letsvpn.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("merchant_profile")
public class MerchantProfile {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("merchant_code")
    private String merchantCode;

    @TableField("platform_id")
    private Integer platformId;

    @TableField("name")
    private String name;

    @TableField("region")
    private String region;

    @TableField("business_type")
    private String businessType;

    @TableField("tier")
    private String tier;

    @TableField("status")
    private String status;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("tags")
    private String tags;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_info")
    private String contactInfo;

    @TableField("extra")
    private String extra;

    @TableField("created_at")
    private Date createdAt;

    @TableField("updated_at")
    private Date updatedAt;
}
