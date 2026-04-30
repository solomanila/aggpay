package com.letsvpn.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserInfoVO {
    private String username;
    private Integer level;
    private LocalDateTime vipExpireTime;
    private String type; // VIP套餐类型，如"青铜"、"白金"等，未购买VIP时为null
}
