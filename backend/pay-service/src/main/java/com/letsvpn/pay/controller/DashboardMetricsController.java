package com.letsvpn.pay.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.common.core.dto.BoardChannelDTO;
import com.letsvpn.common.core.dto.ChannelOpenRateRow;
import com.letsvpn.common.core.dto.ChannelPlatformStatDTO;
import com.letsvpn.common.core.dto.ChannelSuccessRatePoint;
import com.letsvpn.common.core.dto.DashboardSummaryResponse;
import com.letsvpn.common.core.dto.HomeDashboardMetricsResponse;
import com.letsvpn.common.core.dto.MerchantProfileDTO;
import com.letsvpn.common.core.dto.OrderBuildErrorDTO;
import com.letsvpn.common.core.dto.OrderInfoDTO;
import com.letsvpn.common.core.dto.PayConfigChannelDTO;
import com.letsvpn.common.core.dto.PayConfigChannelUpdateRequest;
import com.letsvpn.common.core.dto.PayConfigInfoDTO;
import com.letsvpn.common.core.response.R;
import com.letsvpn.pay.service.core.DashboardMetricsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供给 admin-service 调用的首页指标接口。
 */
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class DashboardMetricsController {

    private final DashboardMetricsService dashboardMetricsService;

    @GetMapping("/home/metrics")
    public R<HomeDashboardMetricsResponse> getHomeMetrics() {
        HomeDashboardMetricsResponse metrics = dashboardMetricsService.getHomeMetrics();
        return R.success(metrics);
    }

    @GetMapping("/dashboard/summary")
    public R<DashboardSummaryResponse> getDashboardSummary() {
        return R.success(dashboardMetricsService.getDashboardSummary());
    }

    @GetMapping("/dashboard/merchants")
    public R<List<String>> listMerchantAppIds() {
        return R.success(dashboardMetricsService.listMerchantAppIds());
    }

    @GetMapping("/dashboard/payConfigIds")
    public R<List<Long>> listPayConfigIds() {
        return R.success(dashboardMetricsService.listPayConfigIds());
    }

    @GetMapping("/dashboard/channelOpenRate")
    public R<List<ChannelOpenRateRow>> getChannelOpenRate(
            @RequestParam(value = "minutes", defaultValue = "30") int minutes) {
        return R.success(dashboardMetricsService.getChannelOpenRate(minutes));
    }

    @GetMapping("/dashboard/channelSuccessRate")
    public R<List<ChannelSuccessRatePoint>> getChannelSuccessRate(
            @RequestParam(value = "date", defaultValue = "today") String date) {
        return R.success(dashboardMetricsService.getChannelSuccessRate(date));
    }

    @GetMapping("/dashboard/channelConfigList")
    public R<Page<PayConfigChannelDTO>> getChannelConfigList(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "nullify", required = false) Integer nullify,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        return R.success(dashboardMetricsService.getChannelConfigList(title, nullify, pageNum, pageSize));
    }

    @PutMapping("/dashboard/channelConfigUpdate")
    public R<Void> updateChannelConfig(@RequestBody PayConfigChannelUpdateRequest req) {
        dashboardMetricsService.updateChannelConfig(req);
        return R.success(null);
    }

    @PatchMapping("/dashboard/channelConfigNullify")
    public R<Void> updateChannelNullify(
            @RequestParam("id") Integer id,
            @RequestParam("nullify") Integer nullify) {
        dashboardMetricsService.updateChannelNullify(id, nullify);
        return R.success(null);
    }

    @GetMapping("/dashboard/payConfigInfoList")
    public R<Page<PayConfigInfoDTO>> getPayConfigInfoList(
            @RequestParam(value = "shortCode", required = false) String shortCode,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        return R.success(dashboardMetricsService.getPayConfigInfoList(shortCode, pageNum, pageSize));
    }

    @GetMapping("/dashboard/orderBuildErrorList")
    public R<Page<OrderBuildErrorDTO>> getOrderBuildErrorList(
            @RequestParam(value = "mdcId", required = false) String mdcId,
            @RequestParam(value = "errorText", required = false) String errorText,
            @RequestParam(value = "payConfigId", required = false) Integer payConfigId,
            @RequestParam(value = "appId", required = false) String appId,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        return R.success(dashboardMetricsService.getOrderBuildErrorList(
                mdcId, errorText, payConfigId, appId, pageNum, pageSize));
    }

    @GetMapping("/dashboard/channelStat")
    public R<Page<OrderInfoDTO>> getChannelStats(
            @RequestParam(value = "period", defaultValue = "today") String period,
            @RequestParam(value = "payConfigId", required = false) Integer payConfigId,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        return R.success(dashboardMetricsService.getChannelStats(period, payConfigId, pageNum, pageSize));
    }

    // ── Merchant profile endpoints (for admin-service Feign) ──────

    @GetMapping("/dashboard/merchantProfiles")
    public R<List<MerchantProfileDTO>> listMerchantProfiles() {
        return R.success(dashboardMetricsService.listMerchantProfiles());
    }

    @GetMapping("/dashboard/merchantPage")
    public R<Page<MerchantProfileDTO>> getMerchantPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        return R.success(dashboardMetricsService.getMerchantPage(keyword, status, pageNum, pageSize));
    }

    @GetMapping("/dashboard/merchantDetail/{platformId}")
    public R<MerchantProfileDTO> getMerchantDetail(@PathVariable Integer platformId) {
        return R.success(dashboardMetricsService.getMerchantDetail(platformId));
    }

    @PostMapping("/dashboard/merchantCreate")
    public R<MerchantProfileDTO> createMerchant(
            @RequestParam String title,
            @RequestParam(required = false) Integer status) {
        return R.success(dashboardMetricsService.createMerchant(title, status));
    }

    @PutMapping("/dashboard/merchantUpdate/{platformId}")
    public R<Void> updateMerchant(
            @PathVariable Integer platformId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status) {
        dashboardMetricsService.updateMerchant(platformId, title, status);
        return R.success(null);
    }

    @PostMapping("/dashboard/merchantStatus/{platformId}")
    public R<Void> toggleMerchantStatus(
            @PathVariable Integer platformId,
            @RequestParam Integer status) {
        dashboardMetricsService.toggleMerchantStatus(platformId, status);
        return R.success(null);
    }

    @DeleteMapping("/dashboard/merchant/{platformId}")
    public R<Void> deleteMerchant(@PathVariable Integer platformId) {
        dashboardMetricsService.deleteMerchant(platformId);
        return R.success(null);
    }

    @PostMapping("/dashboard/merchantResetKey/{platformId}")
    public R<String> resetMerchantKey(@PathVariable Integer platformId) {
        return R.success(dashboardMetricsService.resetMerchantKey(platformId));
    }

    // ── Board channel endpoints ───────────────────────────────────

    @GetMapping("/dashboard/activeChannelIds")
    public R<List<Long>> getActiveChannelIds() {
        return R.success(dashboardMetricsService.getActiveChannelIds());
    }

    @GetMapping("/dashboard/channelsByIds")
    public R<List<BoardChannelDTO>> getChannelsByIds(@RequestParam List<Long> ids) {
        return R.success(dashboardMetricsService.getChannelsByIds(ids));
    }

    @GetMapping("/dashboard/dailyChannelPlatformStats")
    public R<List<ChannelPlatformStatDTO>> getDailyChannelPlatformStats(
            @RequestParam("date") String date) {
        return R.success(dashboardMetricsService.getDailyChannelPlatformStats(date));
    }
}
