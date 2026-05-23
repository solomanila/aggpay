package com.letsvpn.admin.client;

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
import com.letsvpn.common.core.dto.BillOrderDetailDTO;
import com.letsvpn.common.core.dto.BillOrderIdsDTO;
import com.letsvpn.common.core.dto.OrderCallbackDTO;
import com.letsvpn.common.core.dto.PayConfigChannelSaveRequest;
import com.letsvpn.common.core.dto.PayConfigChannelUpdateRequest;
import com.letsvpn.common.core.dto.PayConfigInfoDTO;
import com.letsvpn.common.core.response.R;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 基础的 pay-service OpenFeign 客户端，后续可以在这里继续扩展。
 */
@FeignClient(
        name = "pay-service",
        path = "/api/pay",
        contextId = "payServiceClient",
        fallbackFactory = PayServiceClientFallbackFactory.class)
public interface PayServiceClient {

    @GetMapping("/health/ping")
    String ping(@RequestParam(value = "echo", required = false) String echo);

    @GetMapping("/home/metrics")
    R<HomeDashboardMetricsResponse> getHomeMetrics();

    @GetMapping("/dashboard/summary")
    R<DashboardSummaryResponse> getDashboardSummary();

    @GetMapping("/dashboard/merchants")
    R<List<String>> getMerchantAppIds();

    @GetMapping("/dashboard/payConfigIds")
    R<List<Long>> getPayConfigIds();

    @GetMapping("/dashboard/channelOpenRate")
    R<List<ChannelOpenRateRow>> getChannelOpenRate(
            @RequestParam(value = "minutes", defaultValue = "30") int minutes);

    @GetMapping("/dashboard/channelSuccessRate")
    R<List<ChannelSuccessRatePoint>> getChannelSuccessRate(
            @RequestParam(value = "date", defaultValue = "today") String date,
            @RequestParam(value = "test", required = false) Integer test,
            @RequestParam(value = "merchant", required = false) String merchant);

    @GetMapping("/dashboard/channelConfigList")
    R<Page<PayConfigChannelDTO>> getChannelConfigList(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "nullify", required = false) Integer nullify,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize);

    @PutMapping("/dashboard/channelConfigUpdate")
    R<Void> updateChannelConfig(@RequestBody PayConfigChannelUpdateRequest req);

    @PatchMapping("/dashboard/channelConfigNullify")
    R<Void> updateChannelNullify(
            @RequestParam("id") Integer id,
            @RequestParam("nullify") Integer nullify);

    @GetMapping("/dashboard/payConfigInfoList")
    R<Page<PayConfigInfoDTO>> getPayConfigInfoList(
            @RequestParam(value = "shortCode", required = false) String shortCode,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize);

    @PostMapping("/dashboard/payConfigInfoCreate")
    R<Void> createPayConfigInfo(
            @RequestParam("title") String title,
            @RequestParam("url") String url);

    @PutMapping("/dashboard/payConfigInfoUpdate")
    R<Void> updatePayConfigInfo(
            @RequestParam("id") Integer id,
            @RequestParam("title") String title,
            @RequestParam("url") String url);

    @GetMapping("/dashboard/payConfigInfoOptions")
    R<List<PayConfigInfoDTO>> getPayConfigInfoOptions();

    @PostMapping("/dashboard/payConfigChannelCreate")
    R<Void> createPayConfigChannel(@RequestBody PayConfigChannelSaveRequest req);

    @PutMapping("/dashboard/payConfigChannelUpdate")
    R<Void> updatePayConfigChannel(@RequestBody PayConfigChannelSaveRequest req);

    @GetMapping("/dashboard/orderSumByChannel")
    R<BigDecimal> getOrderSumByChannel(
            @RequestParam("channelId") Long channelId,
            @RequestParam("startTimeMillis") Long startTimeMillis);

    @GetMapping("/dashboard/channelTitleById")
    R<String> getChannelTitleById(@RequestParam("channelId") Long channelId);

    @PutMapping("/dashboard/updateOrderOnlineId")
    R<Void> updateOrderOnlineId(
            @RequestParam("channelId") Long channelId,
            @RequestParam("startTimeMillis") Long startTimeMillis,
            @RequestParam("billId") Long billId);

    @PostMapping("/dashboard/manualCallback")
    R<Void> manualCallback(@RequestParam("orderId") String orderId);

    @GetMapping("/dashboard/orderDetailsByBillId")
    R<List<BillOrderDetailDTO>> getOrderDetailsByBillId(@RequestParam("billId") Long billId);

    @PostMapping("/dashboard/orderIdsByBillIds")
    R<List<BillOrderIdsDTO>> getOrderIdsByBillIds(@RequestBody List<Long> billIds);

    @GetMapping("/dashboard/orderBuildErrorList")
    R<Page<OrderBuildErrorDTO>> getOrderBuildErrorList(
            @RequestParam(value = "mdcId", required = false) String mdcId,
            @RequestParam(value = "errorText", required = false) String errorText,
            @RequestParam(value = "payConfigId", required = false) Integer payConfigId,
            @RequestParam(value = "appId", required = false) String appId,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize);

    @GetMapping("/dashboard/channelStat")
    R<Page<PayinOrderVO>> getChannelStats(
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
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize);

    @GetMapping("/dashboard/channelStatPayout")
    R<Page<PayinOrderVO>> getChannelStatsPayout(
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
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize);

    @GetMapping("/dashboard/allChannelOptions")
    R<List<BoardChannelDTO>> getAllChannelOptions();

    // ── Merchant profile endpoints ────────────────────────────────

    @GetMapping("/dashboard/merchantProfiles")
    R<List<MerchantProfileDTO>> getMerchantProfiles();

    @GetMapping("/dashboard/merchantPage")
    R<Page<MerchantProfileDTO>> getMerchantPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize);

    @GetMapping("/dashboard/merchantDetail/{platformId}")
    R<MerchantProfileDTO> getMerchantDetail(@PathVariable("platformId") Integer platformId);

    @PostMapping("/dashboard/merchantCreate")
    R<MerchantProfileDTO> createMerchant(
            @RequestParam("title") String title,
            @RequestParam(value = "status", required = false) Integer status);

    @PutMapping("/dashboard/merchantUpdate/{platformId}")
    R<Void> updateMerchant(
            @PathVariable("platformId") Integer platformId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "status", required = false) Integer status);

    @PostMapping("/dashboard/merchantStatus/{platformId}")
    R<Void> toggleMerchantStatus(
            @PathVariable("platformId") Integer platformId,
            @RequestParam("status") Integer status);

    @DeleteMapping("/dashboard/merchant/{platformId}")
    R<Void> deleteMerchant(@PathVariable("platformId") Integer platformId);

    @PostMapping("/dashboard/merchantResetKey/{platformId}")
    R<String> resetMerchantKey(@PathVariable("platformId") Integer platformId);

    // ── Merchant portal ──────────────────────────────────────────────

    @GetMapping("/merchant/totalIncome")
    R<BigDecimal> getMerchantTotalIncome(@RequestParam("platformId") Integer platformId);

    @GetMapping("/merchant/platformInfo/{platformId}")
    R<MerchantPlatformInfoDTO> getMerchantPlatformInfo(
            @PathVariable("platformId") Integer platformId);

    @GetMapping("/merchant/orders/payin")
    R<Page<OrderInfoDTO>> getMerchantPayinOrders(
            @RequestParam("platformId") Integer platformId,
            @RequestParam(value = "orderId", required = false) String orderId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize);

    // ── Board channel endpoint ────────────────────────────────────

    @GetMapping("/dashboard/activeChannelIds")
    R<List<Long>> getActiveChannelIds();

    @GetMapping("/dashboard/channelsByIds")
    R<List<BoardChannelDTO>> getChannelsByIds(@RequestParam("ids") List<Long> ids);

    @GetMapping("/dashboard/dailyChannelPlatformStats")
    R<List<ChannelPlatformStatDTO>> getDailyChannelPlatformStats(@RequestParam("date") String date);

    @GetMapping("/dashboard/payChannelPage")
    R<Page<PayChannelPageRowDTO>> getPayChannelPage(
            @RequestParam(value = "id",       required = false) Long id,
            @RequestParam(value = "title",    required = false) String title,
            @RequestParam(value = "status",   required = false) Integer status,
            @RequestParam(value = "areaType", required = false) Integer areaType,
            @RequestParam(value = "pageNum",  defaultValue = "1")  long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize);

    @GetMapping("/dashboard/payinSummaryPage")
    R<Page<PayinSummaryRowDTO>> getPayinSummaryPage(
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "areaType",  required = false) Integer areaType,
            @RequestParam(value = "pageNum",   defaultValue = "1")  long pageNum,
            @RequestParam(value = "pageSize",  defaultValue = "20") long pageSize);

    @GetMapping("/dashboard/payoutSummaryPage")
    R<Page<PayinSummaryRowDTO>> getPayoutSummaryPage(
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "areaType",  required = false) Integer areaType,
            @RequestParam(value = "pageNum",   defaultValue = "1")  long pageNum,
            @RequestParam(value = "pageSize",  defaultValue = "20") long pageSize);

    @GetMapping("/dashboard/orderById")
    R<OrderInfoDTO> getOrderInfoById(@RequestParam("id") Long id);

    @PostMapping("/dashboard/ordersByOrderIds")
    R<List<OrderInfoDTO>> getOrdersByOrderIds(@RequestBody List<String> orderIds);

    @GetMapping("/dashboard/orderCallbackList")
    R<List<OrderCallbackDTO>> getOrderCallbackList(@RequestParam("orderId") String orderId);

    @GetMapping("/req")
    String payTestReq(
            @RequestParam("fid") String fid,
            @RequestParam("uid") Long uid,
            @RequestParam("amount") String amount,
            @RequestParam("pf") String pf,
            @RequestParam("sign") String sign,
            @RequestParam("time") Long time,
            @RequestParam("cid") Long cid);
}
