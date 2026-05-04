package com.letsvpn.pay.client;

import com.letsvpn.common.core.response.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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
        };
    }
}
