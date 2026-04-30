package com.letsvpn.admin.controller;

import com.letsvpn.admin.dto.MerchantBalanceOpRequest;
import com.letsvpn.admin.entity.MerchantBalance;
import com.letsvpn.admin.entity.MerchantBalanceLog;
import com.letsvpn.admin.service.MerchantBalanceService;
import com.letsvpn.common.core.response.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商户余额管理（多币种）
 *
 * 操作类型：充值 recharge / 扣减 deduct / 冻结 freeze / 解冻 unfreeze / 提现 withdraw
 */
@RestController
@RequestMapping("/admin/merchant/balance")
@RequiredArgsConstructor
public class MerchantBalanceController {

    private final MerchantBalanceService service;

    /** 查询商户所有币种余额 */
    @GetMapping("/{platformId}")
    public R<List<MerchantBalance>> list(@PathVariable Integer platformId) {
        return R.success(service.listByPlatformId(platformId));
    }

    /** 充值 */
    @PostMapping("/recharge")
    public R<Void> recharge(@RequestBody MerchantBalanceOpRequest req) {
        service.recharge(req, null);
        return R.success(null);
    }

    /** 扣减 */
    @PostMapping("/deduct")
    public R<Void> deduct(@RequestBody MerchantBalanceOpRequest req) {
        service.deduct(req, null);
        return R.success(null);
    }

    /** 冻结 */
    @PostMapping("/freeze")
    public R<Void> freeze(@RequestBody MerchantBalanceOpRequest req) {
        service.freeze(req, null);
        return R.success(null);
    }

    /** 解冻 */
    @PostMapping("/unfreeze")
    public R<Void> unfreeze(@RequestBody MerchantBalanceOpRequest req) {
        service.unfreeze(req, null);
        return R.success(null);
    }

    /** 提现 */
    @PostMapping("/withdraw")
    public R<Void> withdraw(@RequestBody MerchantBalanceOpRequest req) {
        service.withdraw(req, null);
        return R.success(null);
    }

    /** 余额流水查询 */
    @GetMapping("/logs/{platformId}")
    public R<List<MerchantBalanceLog>> logs(
            @PathVariable Integer platformId,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.success(service.listLogs(platformId, currency, page, size));
    }
}
