package com.letsvpn.pay.client;

import com.letsvpn.common.core.dto.OrderCreatedAtDTO;
import com.letsvpn.common.core.response.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class AdminBalanceLogInternalClientFallbackFactory
        implements FallbackFactory<AdminBalanceLogInternalClient> {

    @Override
    public AdminBalanceLogInternalClient create(Throwable cause) {
        log.warn("admin-service balance-log feign call failed", cause);
        return orderIds -> R.success(Collections.emptyList());
    }
}
