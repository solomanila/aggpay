package com.letsvpn.pay.client;

import com.letsvpn.common.core.dto.MerchantChannelConfigDTO;
import com.letsvpn.common.core.response.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class AdminServiceClientFallbackFactory implements FallbackFactory<AdminServiceClient> {

    @Override
    public AdminServiceClient create(Throwable cause) {
        log.warn("admin-service feign call failed, entering fallback", cause);
        return new AdminServiceClient() {
            @Override
            public R<List<MerchantChannelConfigDTO>> getMerchantChannelConfigs(
                    Integer platformId, String channelType) {
                return R.success(Collections.emptyList());
            }

            @Override
            public R<Void> disableMerchantChannelConfig(Long id) {
                log.warn("admin-service unavailable, skip disable for config id={}", id);
                return R.success(null);
            }
        };
    }
}
