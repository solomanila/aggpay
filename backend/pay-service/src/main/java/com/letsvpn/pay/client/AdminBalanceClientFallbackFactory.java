package com.letsvpn.pay.client;

import com.letsvpn.common.core.dto.MerchantBalanceDTO;
import com.letsvpn.common.core.response.R;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AdminBalanceClientFallbackFactory implements FallbackFactory<AdminBalanceClient> {

    @Override
    public AdminBalanceClient create(Throwable cause) {
        log.warn("admin-service balance feign call failed, entering fallback", cause);
        return new AdminBalanceClient() {
            @Override
            public R<Void> creditOrderIncome(Integer platformId, BigDecimal amount) {
                log.error("creditOrderIncome fallback: platformId={}, amount={}", platformId, amount);
                return R.fail("admin-service unavailable");
            }

            @Override
            public R<List<MerchantBalanceDTO>> getBalancesByPlatformIds(List<Integer> platformIds) {
                return R.success(Collections.emptyList());
            }
        };
    }
}
