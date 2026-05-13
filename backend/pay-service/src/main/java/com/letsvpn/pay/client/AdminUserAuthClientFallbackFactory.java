package com.letsvpn.pay.client;

import com.letsvpn.common.core.dto.PlatformAccountDTO;
import com.letsvpn.common.core.response.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class AdminUserAuthClientFallbackFactory implements FallbackFactory<AdminUserAuthClient> {

    @Override
    public AdminUserAuthClient create(Throwable cause) {
        log.warn("admin-service user-auth feign call failed", cause);
        return new AdminUserAuthClient() {
            @Override
            public R<List<PlatformAccountDTO>> getAccountsByPlatformIds(List<Integer> platformIds) {
                return R.success(Collections.emptyList());
            }

            @Override
            public R<Integer> getPlatformIdByAccount(String account) {
                return R.success(null);
            }
        };
    }
}
