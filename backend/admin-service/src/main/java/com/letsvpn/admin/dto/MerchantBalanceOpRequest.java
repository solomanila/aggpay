package com.letsvpn.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 余额操作请求：充值/扣减/冻结/解冻/提现 */
@Data
public class MerchantBalanceOpRequest {
    /** 商户 platform_id */
    private Integer platformId;
    /** 货币代码 INR/RUB 等 */
    private String currency;
    /** 操作金额（正数） */
    private BigDecimal amount;
    /** 备注 */
    private String remark;
}
