package com.letsvpn.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.admin.dto.MerchantBalanceOpRequest;
import com.letsvpn.admin.dto.MerchantWithdrawRequest;
import com.letsvpn.admin.entity.MerchantWithdraw;
import com.letsvpn.admin.entity.SystemUserAuth;
import com.letsvpn.admin.mapper.MerchantWithdrawMapper;
import com.letsvpn.admin.mapper.SystemUserAuthMapper;
import com.letsvpn.admin.service.MerchantBalanceService;
import com.letsvpn.common.core.response.R;
import com.letsvpn.common.core.util.AuthContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@RestController
@RequestMapping("/admin/merchant/withdraw")
@RequiredArgsConstructor
public class MerchantWithdrawController {

    private final SystemUserAuthMapper userAuthMapper;
    private final MerchantWithdrawMapper withdrawMapper;
    private final MerchantBalanceService balanceService;

    @PostMapping
    public R<Void> createWithdraw(@RequestBody MerchantWithdrawRequest req) {
        Long userId = AuthContextHolder.getRequiredUserId();
        SystemUserAuth user = userAuthMapper.selectById(userId);
        if (user == null || user.getPlatformId() == null) {
            return R.fail("未绑定商户平台");
        }
        if (req.getAmount() == null || req.getAmount().signum() <= 0) {
            return R.fail("提现金额必须大于0");
        }
        if (req.getUsdtAddress() == null || req.getUsdtAddress().isBlank()) {
            return R.fail("USDT地址不能为空");
        }

        String currency = (req.getCurrency() != null && !req.getCurrency().isBlank())
                ? req.getCurrency() : "INR";

        MerchantBalanceOpRequest balanceReq = new MerchantBalanceOpRequest();
        balanceReq.setPlatformId(user.getPlatformId());
        balanceReq.setCurrency(currency);
        balanceReq.setAmount(req.getAmount());
        balanceReq.setRemark("提现申请");
        balanceService.withdraw(balanceReq, userId);

        MerchantWithdraw withdraw = new MerchantWithdraw();
        withdraw.setPlatformId(user.getPlatformId());
        withdraw.setCurrency(currency);
        withdraw.setStatus(0);
        withdraw.setAmount(req.getAmount());
        withdraw.setUsdtAddress(req.getUsdtAddress().trim());
        withdraw.setCreateTime(new Date());
        withdrawMapper.insert(withdraw);

        return R.success(null);
    }

    @GetMapping("/page")
    public R<Page<MerchantWithdraw>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long userId = AuthContextHolder.getRequiredUserId();
        SystemUserAuth user = userAuthMapper.selectById(userId);
        if (user == null || user.getPlatformId() == null) {
            return R.fail("未绑定商户平台");
        }

        long pi = Math.max(pageNum, 1L);
        long ps = Math.max(1L, Math.min(pageSize, 100L));

        LambdaQueryWrapper<MerchantWithdraw> qw = new LambdaQueryWrapper<MerchantWithdraw>()
                .eq(MerchantWithdraw::getPlatformId, user.getPlatformId())
                .eq(status != null, MerchantWithdraw::getStatus, status)
                .orderByDesc(MerchantWithdraw::getCreateTime);

        if (startDate != null && !startDate.isBlank()) {
            try {
                Date start = Date.from(LocalDate.parse(startDate)
                        .atStartOfDay(ZoneId.systemDefault()).toInstant());
                qw.ge(MerchantWithdraw::getCreateTime, start);
            } catch (Exception ignored) {}
        }
        if (endDate != null && !endDate.isBlank()) {
            try {
                Date end = Date.from(LocalDate.parse(endDate).plusDays(1)
                        .atStartOfDay(ZoneId.systemDefault()).toInstant());
                qw.lt(MerchantWithdraw::getCreateTime, end);
            } catch (Exception ignored) {}
        }

        return R.success(withdrawMapper.selectPage(new Page<>(pi, ps), qw));
    }
}
