package com.letsvpn.common.core.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MerchantBalanceDTO {
    private Integer    platformId;
    private String     currency;
    private BigDecimal available;
    private BigDecimal frozen;
    private BigDecimal payoutAvailable;
    private BigDecimal payoutFrozen;
    private String account;
}
