package com.letsvpn.admin.controller;

import com.letsvpn.admin.service.PayServiceFacade;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.common.core.dto.ChannelOpenRateRow;
import com.letsvpn.common.core.dto.ChannelSuccessRatePoint;
import com.letsvpn.common.core.dto.DashboardSummaryResponse;
import com.letsvpn.common.core.dto.HomeDashboardMetricsResponse;
import com.letsvpn.common.core.dto.OrderBuildErrorDTO;
import com.letsvpn.common.core.dto.OrderInfoDTO;
import com.letsvpn.common.core.dto.PayConfigChannelDTO;
import com.letsvpn.common.core.dto.PayConfigChannelUpdateRequest;
import com.letsvpn.common.core.dto.PayConfigInfoDTO;
import com.letsvpn.common.core.response.R;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
            @RequestParam(value = "date", defaultValue = "today") String date) {
        return R.success(payServiceFacade.fetchChannelSuccessRate(date));
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
    public R<Page<OrderInfoDTO>> dashboardChannelStat(
            @RequestParam(value = "period", defaultValue = "today") String period,
            @RequestParam(value = "payConfigId", required = false) Integer payConfigId,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        return R.success(payServiceFacade.fetchChannelStats(period, payConfigId, pageNum, pageSize));
    }
}
