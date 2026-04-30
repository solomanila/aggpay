// 文件路径: user-service/src/main/java/com/letsvpn/user/dto/CurrentUserVipProfileResponse.java
package com.letsvpn.user.dto;

import com.letsvpn.user.enums.VipLevel;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CurrentUserVipProfileResponse {
    private Long userId;
    private String username;
    private VipLevel currentVipLevel;
    private String vipLevelDescription;
    private LocalDateTime vipExpireTime;
    private boolean isActiveVip;
    private Long daysRemaining;
    private String type; // VIP套餐类型，如"青铜"、"白金"等，未购买VIP时为null
}