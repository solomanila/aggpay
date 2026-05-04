package com.letsvpn.common.core.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class MerchantDashboardResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 各币种余额列表 */
    private List<BalanceItem> balances;

    /** 商户 keyId（aggpay.pay_platform_info.platform_no） */
    private String keyId;

    /** 商户 appKey（aggpay.pay_platform_info.secret_key） */
    private String appKey;

    /** 待结算金额（实时：总收入 - 总提现） */
    private BigDecimal pendingSettlement;

    @Data
    public static class BalanceItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String currency;

        /** 出款（可提现余额） */
        private BigDecimal available;

        /** 待结算（冻结金额） */
        private BigDecimal frozen;
    }
}
