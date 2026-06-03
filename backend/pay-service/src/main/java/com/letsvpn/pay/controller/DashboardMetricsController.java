package com.letsvpn.pay.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import com.letsvpn.common.core.dto.BoardChannelDTO;
import com.letsvpn.common.core.dto.ChannelOpenRateRow;
import com.letsvpn.common.core.dto.ChannelPlatformStatDTO;
import com.letsvpn.common.core.dto.ChannelSuccessRatePoint;
import com.letsvpn.common.core.dto.DashboardSummaryResponse;
import com.letsvpn.common.core.dto.HomeDashboardMetricsResponse;
import com.letsvpn.common.core.dto.MerchantPlatformInfoDTO;
import com.letsvpn.common.core.dto.MerchantProfileDTO;
import com.letsvpn.common.core.dto.OrderBuildErrorDTO;
import com.letsvpn.common.core.dto.OrderInfoDTO;
import com.letsvpn.common.core.dto.PayinOrderVO;
import com.letsvpn.common.core.dto.PayChannelPageRowDTO;
import com.letsvpn.common.core.dto.PayinSummaryRowDTO;
import com.letsvpn.common.core.dto.PayConfigChannelDTO;
import java.math.BigDecimal;
import java.util.Date;
import com.letsvpn.common.core.dto.BillOrderDetailDTO;
import com.letsvpn.common.core.dto.BillOrderIdsDTO;
import com.letsvpn.common.core.dto.OrderCallbackDTO;
import com.letsvpn.common.core.dto.PayConfigChannelSaveRequest;
import com.letsvpn.common.core.dto.PayConfigChannelUpdateRequest;
import com.letsvpn.common.core.dto.PayConfigInfoDTO;
import com.letsvpn.common.core.response.R;
import com.letsvpn.pay.service.core.DashboardMetricsService;
import com.letsvpn.pay.service.core.ManualCallbackService;
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
    private final ManualCallbackService   manualCallbackService;

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
            @RequestParam(value = "date", defaultValue = "today") String date,
            @RequestParam(value = "test", required = false) Integer test,
            @RequestParam(value = "merchant", required = false) String merchant) {
        return R.success(dashboardMetricsService.getChannelSuccessRate(date, test, merchant));
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

    @PostMapping("/dashboard/payConfigInfoCreate")
    public R<Void> createPayConfigInfo(
            @RequestParam String title,
            @RequestParam String url) {
        dashboardMetricsService.createPayConfigInfo(title, url);
        return R.success(null);
    }

    @PutMapping("/dashboard/payConfigInfoUpdate")
    public R<Void> updatePayConfigInfo(
            @RequestParam Integer id,
            @RequestParam String title,
            @RequestParam String url) {
        dashboardMetricsService.updatePayConfigInfo(id, title, url);
        return R.success(null);
    }

    @GetMapping("/dashboard/payConfigInfoOptions")
    public R<List<PayConfigInfoDTO>> getPayConfigInfoOptions() {
        return R.success(dashboardMetricsService.getPayConfigInfoOptions());
    }

    @PostMapping("/dashboard/payConfigChannelCreate")
    public R<Void> createPayConfigChannel(@RequestBody PayConfigChannelSaveRequest req) {
        dashboardMetricsService.createPayConfigChannel(req);
        return R.success(null);
    }

    @PutMapping("/dashboard/payConfigChannelUpdate")
    public R<Void> updatePayConfigChannel(@RequestBody PayConfigChannelSaveRequest req) {
        dashboardMetricsService.updatePayConfigChannel(req);
        return R.success(null);
    }

    @GetMapping("/dashboard/orderSumByChannel")
    public R<BigDecimal> getOrderSumByChannel(
            @RequestParam Long channelId,
            @RequestParam Long startTimeMillis) {
        return R.success(dashboardMetricsService.getOrderSumByChannel(channelId, new Date(startTimeMillis)));
    }

    @GetMapping("/dashboard/channelTitleById")
    public R<String> getChannelTitleById(@RequestParam Long channelId) {
        return R.success(dashboardMetricsService.getChannelTitleById(channelId));
    }

    @PutMapping("/dashboard/updateOrderOnlineId")
    public R<Void> updateOrderOnlineId(
            @RequestParam Long channelId,
            @RequestParam Long startTimeMillis,
            @RequestParam Long billId) {
        dashboardMetricsService.updateOrderOnlineId(channelId, new Date(startTimeMillis), billId);
        return R.success(null);
    }

    @PostMapping("/dashboard/manualCallback")
    public R<Void> manualCallback(@RequestParam String orderId) {
        try {
            manualCallbackService.manualCallback(orderId);
            return R.success(null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/dashboard/orderDetailsByBillId")
    public R<List<BillOrderDetailDTO>> getOrderDetailsByBillId(@RequestParam Long billId) {
        return R.success(dashboardMetricsService.getOrderDetailsByBillId(billId));
    }

    @PostMapping("/dashboard/orderIdsByBillIds")
    public R<List<BillOrderIdsDTO>> getOrderIdsByBillIds(@RequestBody List<Long> billIds) {
        return R.success(dashboardMetricsService.getOrderIdsByBillIds(billIds));
    }

    @GetMapping("/dashboard/orderCallbackList")
    public R<List<OrderCallbackDTO>> getOrderCallbackList(@RequestParam String orderId) {
        return R.success(dashboardMetricsService.getOrderCallbackList(orderId));
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
    public R<Page<PayinOrderVO>> getChannelStats(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "otherOrderId", required = false) String otherOrderId,
            @RequestParam(value = "createStartTime", required = false) String createStartTime,
            @RequestParam(value = "createEndTime", required = false) String createEndTime,
            @RequestParam(value = "payStartTime", required = false) String payStartTime,
            @RequestParam(value = "payEndTime", required = false) String payEndTime,
            @RequestParam(value = "channelId", required = false) Long channelId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        return R.success(dashboardMetricsService.getChannelStats(
                id, otherOrderId, createStartTime, createEndTime,
                payStartTime, payEndTime, channelId, status, account, pageNum, pageSize));
    }

    @GetMapping("/dashboard/channelStatPayout")
    public R<Page<PayinOrderVO>> getChannelStatsPayout(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "otherOrderId", required = false) String otherOrderId,
            @RequestParam(value = "createStartTime", required = false) String createStartTime,
            @RequestParam(value = "createEndTime", required = false) String createEndTime,
            @RequestParam(value = "payStartTime", required = false) String payStartTime,
            @RequestParam(value = "payEndTime", required = false) String payEndTime,
            @RequestParam(value = "channelId", required = false) Long channelId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        return R.success(dashboardMetricsService.getChannelStatsPayout(
                id, otherOrderId, createStartTime, createEndTime,
                payStartTime, payEndTime, channelId, status, account, pageNum, pageSize));
    }

    @GetMapping("/dashboard/allChannelOptions")
    public R<List<BoardChannelDTO>> getAllChannelOptions() {
        return R.success(dashboardMetricsService.getAllChannelOptions());
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

    @GetMapping("/dashboard/orderById")
    public R<OrderInfoDTO> getOrderInfoById(@RequestParam("id") Long id) {
        return R.success(dashboardMetricsService.getOrderInfoById(id));
    }

    @GetMapping("/dashboard/orderByOrderId")
    public R<OrderInfoDTO> getOrderByOrderId(@RequestParam("orderId") String orderId) {
        return R.success(dashboardMetricsService.getOrderByOrderId(orderId));
    }

    @PostMapping("/dashboard/ordersByOrderIds")
    public R<List<OrderInfoDTO>> getOrdersByOrderIds(@RequestBody List<String> orderIds) {
        return R.success(dashboardMetricsService.getOrdersByOrderIds(orderIds));
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

    @GetMapping("/dashboard/payChannelPage")
    public R<Page<PayChannelPageRowDTO>> getPayChannelPage(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer areaType,
            @RequestParam(defaultValue = "1")  long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return R.success(dashboardMetricsService
                .getPayChannelPage(id, title, status, areaType, pageNum, pageSize));
    }

    @GetMapping("/dashboard/payinSummaryPage")
    public R<Page<PayinSummaryRowDTO>> getPayinSummaryPage(
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "areaType",  required = false) Integer areaType,
            @RequestParam(value = "pageNum",   defaultValue = "1")  long pageNum,
            @RequestParam(value = "pageSize",  defaultValue = "20") long pageSize) {
        return R.success(dashboardMetricsService
                .getPayinSummaryPage(startTime, areaType, pageNum, pageSize));
    }

    @GetMapping("/dashboard/payoutSummaryPage")
    public R<Page<PayinSummaryRowDTO>> getPayoutSummaryPage(
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "areaType",  required = false) Integer areaType,
            @RequestParam(value = "pageNum",   defaultValue = "1")  long pageNum,
            @RequestParam(value = "pageSize",  defaultValue = "20") long pageSize) {
        return R.success(dashboardMetricsService
                .getPayoutSummaryPage(startTime, areaType, pageNum, pageSize));
    }

    @GetMapping("/dashboard/dailyChannelPlatformStats")
    public R<List<ChannelPlatformStatDTO>> getDailyChannelPlatformStats(
            @RequestParam("date") String date) {
        return R.success(dashboardMetricsService.getDailyChannelPlatformStats(date));
    }

    // ── Merchant portal ──────────────────────────────────────────────

    @GetMapping("/merchant/totalIncome")
    public R<BigDecimal> getMerchantTotalIncome(@RequestParam("platformId") Integer platformId) {
        return R.success(dashboardMetricsService.getMerchantTotalIncome(platformId));
    }

    @GetMapping("/merchant/platformInfo/{platformId}")
    public R<MerchantPlatformInfoDTO> getMerchantPlatformInfo(
            @PathVariable("platformId") Integer platformId) {
        return R.success(dashboardMetricsService.getMerchantPlatformInfo(platformId));
    }

    @GetMapping("/merchant/orders/payin")
    public R<Page<OrderInfoDTO>> getMerchantPayinOrders(
            @RequestParam("platformId") Integer platformId,
            @RequestParam(value = "orderId", required = false) String orderId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return R.success(dashboardMetricsService.getMerchantPayinOrders(
                platformId, orderId, startDate, endDate, status, pageNum, pageSize));
    }
}
