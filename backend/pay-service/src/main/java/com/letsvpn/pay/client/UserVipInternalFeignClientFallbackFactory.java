package com.letsvpn.pay.client;

import com.letsvpn.common.core.dto.ActivateVipSubscriptionRequest;
import com.letsvpn.common.core.dto.ActivateVipSubscriptionResponse;
import com.letsvpn.common.core.response.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 激活 VIP 调用的兜底逻辑，避免链路波及。
 */
@Component
@Slf4j
public class UserVipInternalFeignClientFallbackFactory implements FallbackFactory<UserVipInternalFeignClient> {

    @Override
    public UserVipInternalFeignClient create(Throwable cause) {
        log.warn("user-service VIP activation feign call failed", cause);
        return new UserVipInternalFeignClient() {
            @Override
            public R<ActivateVipSubscriptionResponse> activateVip(ActivateVipSubscriptionRequest request) {
                return R.fail("user-service unavailable, skip auto activate");
            }
        };
    }
}
