package com.letsvpn.auth.feign;

import com.letsvpn.auth.dto.UserCreationInternalRequest;
import com.letsvpn.common.core.response.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * user-service Feign 调用的兜底策略。
 */
@Component
@Slf4j
public class UserServiceClientFallbackFactory implements FallbackFactory<UserServiceClient> {

    @Override
    public UserServiceClient create(Throwable cause) {
        log.error("user-service feign call failed", cause);
        return new UserServiceClient() {
            @Override
            public R<Void> setupNewUser(UserCreationInternalRequest request) {
                return R.fail("user-service unavailable");
            }
        };
    }
}
