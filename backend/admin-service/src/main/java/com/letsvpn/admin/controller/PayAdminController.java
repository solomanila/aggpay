package com.letsvpn.admin.controller;

import com.letsvpn.admin.service.PayServiceFacade;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.common.core.dto.ChannelOpenRateRow;
import com.letsvpn.common.core.dto.ChannelSuccessRatePoint;
import com.letsvpn.common.core.dto.DashboardSummaryResponse;
import com.letsvpn.common.core.dto.HomeDashboardMetricsResponse;
import com.letsvpn.common.core.dto.BoardChannelDTO;
import com.letsvpn.common.core.dto.OrderBuildErrorDTO;
import com.letsvpn.common.core.dto.OrderCallbackDTO;
import com.letsvpn.common.core.dto.OrderInfoDTO;
import com.letsvpn.common.core.dto.PayinOrderVO;
import com.letsvpn.common.core.dto.PayChannelPageRowDTO;
import com.letsvpn.common.core.dto.PayinSummaryRowDTO;
import com.letsvpn.common.core.dto.PayConfigChannelDTO;
import com.letsvpn.common.core.dto.PayConfigChannelSaveRequest;
import com.letsvpn.common.core.dto.PayConfigChannelUpdateRequest;
import com.letsvpn.common.core.dto.PayConfigInfoDTO;
import com.letsvpn.common.core.response.R;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/pay")
@RequiredArgsConstructor
public class PayAdminController {

    private final PayServiceFacade payServiceFacade;

    @GetMapping("/ping")
    public ResponseEntity<String> ping(@RequestParam(value = "echo", required = false) String echo) {
        String payload = payServiceFacade.pingPayService(echo);
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/home/metrics")
    public R<HomeDashboardMetricsResponse> homeMetrics() {
        return R.success(payServiceFacade.fetchHomeMetrics());
    }

    @GetMapping("/dashboard/summary")
    public R<DashboardSummaryResponse> dashboardSummary() {
        return R.success(payServiceFacade.fetchDashboardSummary());
    }

    @GetMapping("/dashboard/merchants")
    public R<List<String>> dashboardMerchants() {
        return R.success(payServiceFacade.fetchMerchantAppIds());
    }

    @GetMapping("/dashboard/payConfigIds")
    public R<List<Long>> dashboardPayConfigIds() {
        return R.success(payServiceFacade.fetchPayConfigIds());
    }

    @GetMapping("/dashboard/channelOpenRate")
    public R<List<ChannelOpenRateRow>> dashboardChannelOpenRate(
            @RequestParam(value = "minutes", defaultValue = "30") int minutes) {
        return R.success(payServiceFacade.fetchChannelOpenRate(minutes));
    }

    @GetMapping("/dashboard/channelSuccessRate")
    public R<List<ChannelSuccessRatePoint>> dashboardChannelSuccessRate(
            @RequestParam(value = "date", defaultValue = "today") String date,
            @RequestParam(value = "test", required = false) Integer test,
            @RequestParam(value = "merchant", required = false) String merchant) {
        return R.success(payServiceFacade.fetchChannelSuccessRate(date, test, merchant));
    }

    @GetMapping("/dashboard/channelConfigList")
    public R<Page<PayConfigChannelDTO>> dashboardChannelConfigList(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "nullify", required = false) Integer nullify,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        return R.success(payServiceFacade.fetchChannelConfigList(title, nullify, pageNum, pageSize));
    }

    @PutMapping("/dashboard/channelConfigUpdate")
    public R<Void> dashboardChannelConfigUpdate(@RequestBody PayConfigChannelUpdateRequest req) {
        payServiceFacade.updateChannelConfig(req);
        return R.success(null);
    }

    @PatchMapping("/dashboard/channelConfigNullify")
    public R<Void> dashboardChannelConfigNullify(
            @RequestParam("id") Integer id,
            @RequestParam("nullify") Integer nullify) {
        payServiceFacade.updateChannelNullify(id, nullify);
        return R.success(null);
    }

    @GetMapping("/dashboard/payConfigInfoList")
    public R<Page<PayConfigInfoDTO>> dashboardPayConfigInfoList(
            @RequestParam(value = "shortCode", required = false) String shortCode,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        return R.success(payServiceFacade.fetchPayConfigInfoList(shortCode, pageNum, pageSize));
    }

    @PostMapping("/dashboard/payConfigInfoCreate")
    public R<Void> createPayConfigInfo(
            @RequestParam String title,
            @RequestParam String url) {
        payServiceFacade.createPayConfigInfo(title, url);
        return R.success(null);
    }

    @PutMapping("/dashboard/payConfigInfoUpdate")
    public R<Void> updatePayConfigInfo(
            @RequestParam Integer id,
            @RequestParam String title,
            @RequestParam String url) {
        payServiceFacade.updatePayConfigInfo(id, title, url);
        return R.success(null);
    }

    @GetMapping("/dashboard/orderBuildErrorList")
    public R<Page<OrderBuildErrorDTO>> dashboardOrderBuildErrorList(
            @RequestParam(value = "mdcId", required = false) String mdcId,
            @RequestParam(value = "errorText", required = false) String errorText,
            @RequestParam(value = "payConfigId", required = false) Integer payConfigId,
            @RequestParam(value = "appId", required = false) String appId,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        return R.success(payServiceFacade.fetchOrderBuildErrorList(
                mdcId, errorText, payConfigId, appId, pageNum, pageSize));
    }

    @GetMapping("/dashboard/channelStat")
    public R<Page<PayinOrderVO>> dashboardChannelStat(
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
        return R.success(payServiceFacade.fetchChannelStats(
                id, otherOrderId, createStartTime, createEndTime,
                payStartTime, payEndTime, channelId, status, account, pageNum, pageSize));
    }

    @GetMapping("/dashboard/channelStatPayout")
    public R<Page<PayinOrderVO>> dashboardChannelStatPayout(
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
        return R.success(payServiceFacade.fetchChannelStatsPayout(
                id, otherOrderId, createStartTime, createEndTime,
                payStartTime, payEndTime, channelId, status, account, pageNum, pageSize));
    }

    @GetMapping("/dashboard/allChannelOptions")
    public R<List<BoardChannelDTO>> dashboardAllChannelOptions() {
        return R.success(payServiceFacade.fetchAllChannelOptions());
    }

    @GetMapping("/dashboard/payChannelPage")
    public R<Page<PayChannelPageRowDTO>> dashboardPayChannelPage(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1")  long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return R.success(payServiceFacade
                .fetchPayChannelPage(id, title, status, currency, pageNum, pageSize));
    }

    @GetMapping("/dashboard/payinSummaryPage")
    public R<Page<PayinSummaryRowDTO>> dashboardPayinSummaryPage(
            @RequestParam(required = false) String dateType,
            @RequestParam(required = false) Integer areaType,
            @RequestParam(defaultValue = "1")  long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return R.success(payServiceFacade
                .fetchPayinSummaryPage(dateType, areaType, pageNum, pageSize));
    }

    @GetMapping("/dashboard/payoutSummaryPage")
    public R<Page<PayinSummaryRowDTO>> dashboardPayoutSummaryPage(
            @RequestParam(required = false) String dateType,
            @RequestParam(required = false) Integer areaType,
            @RequestParam(defaultValue = "1")  long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return R.success(payServiceFacade
                .fetchPayoutSummaryPage(dateType, areaType, pageNum, pageSize));
    }

    @GetMapping("/dashboard/payConfigInfoOptions")
    public R<List<PayConfigInfoDTO>> payConfigInfoOptions() {
        return R.success(payServiceFacade.fetchPayConfigInfoOptions());
    }

    @PostMapping("/dashboard/payConfigChannelCreate")
    public R<Void> createPayConfigChannel(@RequestBody PayConfigChannelSaveRequest req) {
        payServiceFacade.createPayConfigChannel(req);
        return R.success(null);
    }

    @PutMapping("/dashboard/payConfigChannelUpdate")
    public R<Void> updatePayConfigChannel(@RequestBody PayConfigChannelSaveRequest req) {
        payServiceFacade.updatePayConfigChannel(req);
        return R.success(null);
    }

    /**
     * 手动回调：对指定订单强制执行支付成功流程（无需三方回调）。
     * 适用于三方已到账但回调丢失的订单补偿场景。
     */
    @PostMapping("/order/manualCallback")
    public R<Void> manualCallback(@RequestParam String orderId) {
        try {
            payServiceFacade.manualCallback(orderId);
            return R.success(null);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/order/callbackList")
    public R<List<OrderCallbackDTO>> orderCallbackList(@RequestParam String orderId) {
        return R.success(payServiceFacade.fetchOrderCallbackList(orderId));
    }
}
