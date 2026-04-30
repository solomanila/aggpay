package com.letsvpn.pay.client;

import com.letsvpn.common.core.dto.ChannelProfitStatDTO;
import com.letsvpn.common.core.response.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class AdminProfitStatClientFallbackFactory implements FallbackFactory<AdminProfitStatClient> {

    @Override
    public AdminProfitStatClient create(Throwable cause) {
        return new AdminProfitStatClient() {
            @Override
            public R<Void> batchSave(List<ChannelProfitStatDTO> stats) {
                log.error("AdminProfitStatClient.batchSave fallback, cause={}", cause.getMessage());
                return R.fail("admin-service unavailable");
            }
        };
    }
}
