package com.letsvpn.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.letsvpn.admin.client.PayServiceClient;
import com.letsvpn.common.core.dto.BoardChannelDTO;
import com.letsvpn.common.core.dto.ChannelOpenRateRow;
import com.letsvpn.common.core.dto.ChannelPlatformStatDTO;
import com.letsvpn.common.core.dto.ChannelSuccessRatePoint;
import com.letsvpn.common.core.dto.DashboardSummaryResponse;
import com.letsvpn.common.core.dto.HomeDashboardMetricsResponse;
import com.letsvpn.common.core.dto.MerchantProfileDTO;
import com.letsvpn.common.core.dto.OrderBuildErrorDTO;
import com.letsvpn.common.core.dto.OrderInfoDTO;
import com.letsvpn.admin.constant.AreaTypeConstants;
import com.letsvpn.admin.entity.SystemUserAuth;
import com.letsvpn.admin.mapper.SystemUserAuthMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import java.util.Date;
import com.letsvpn.common.core.dto.BillOrderDetailDTO;
import com.letsvpn.common.core.dto.BillOrderIdsDTO;
import com.letsvpn.common.core.dto.OrderCallbackDTO;
import com.letsvpn.common.core.dto.PayChannelPageRowDTO;
import com.letsvpn.common.core.dto.PayinOrderVO;
import com.letsvpn.common.core.dto.PayinSummaryRowDTO;
import com.letsvpn.common.core.dto.PayConfigChannelDTO;
import com.letsvpn.common.core.dto.PayConfigChannelSaveRequest;
import com.letsvpn.common.core.dto.PayConfigChannelUpdateRequest;
import com.letsvpn.common.core.dto.PayConfigInfoDTO;
import com.letsvpn.common.core.response.R;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Facade that wraps PayServiceClient so controllers can stay agnostic of Feign details.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayServiceFacade {

    private static final String HOME_METRICS_CACHE_KEY = "payadmin:home:metrics";
    private static final Duration HOME_METRICS_TTL = Duration.ofMinutes(5);
    private static final String DASHBOARD_SUMMARY_CACHE_KEY = "payadmin:dashboard:summary";
    private static final Duration DASHBOARD_SUMMARY_TTL = Duration.ofMinutes(2);

    private final PayServiceClient payServiceClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SystemUserAuthMapper systemUserAuthMapper;

    public String pingPayService(String echo) {
        return payServiceClient.ping(echo);
    }

    public HomeDashboardMetricsResponse fetchHomeMetrics() {
        HomeDashboardMetricsResponse cached = readMetricsFromCache();
        if (cached != null) {
            return cached;
        }

        R<HomeDashboardMetricsResponse> response = payServiceClient.getHomeMetrics();
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            throw new IllegalStateException("Failed to fetch home metrics from pay-service");
        }

        HomeDashboardMetricsResponse data = response.getData();
        cacheMetrics(data);
        return data;
    }

    public DashboardSummaryResponse fetchDashboardSummary() {
        DashboardSummaryResponse cached = readSummaryFromCache();
        if (cached != null) {
            return cached;
        }

        R<DashboardSummaryResponse> response = payServiceClient.getDashboardSummary();
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            throw new IllegalStateException("Failed to fetch dashboard summary from pay-service");
        }
        DashboardSummaryResponse data = response.getData();
        cacheSummary(data);
        return data;
    }

    public List<String> fetchMerchantAppIds() {
        R<List<String>> response = payServiceClient.getMerchantAppIds();

        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch merchant app ids from pay-service, fallback to empty list");
            return Collections.emptyList();
        }
        return response.getData();
    }

    public List<Long> fetchPayConfigIds() {
        R<List<Long>> response = payServiceClient.getPayConfigIds();
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch pay config ids from pay-service, fallback to empty list");
            return Collections.emptyList();
        }
        return response.getData();
    }

    public List<ChannelOpenRateRow> fetchChannelOpenRate(int minutes) {
        R<List<ChannelOpenRateRow>> response = payServiceClient.getChannelOpenRate(minutes);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch channel open rate from pay-service, returning empty list");
            return Collections.emptyList();
        }
        return response.getData();
    }

    public List<ChannelSuccessRatePoint> fetchChannelSuccessRate(String date, Integer test, String merchant) {
        R<List<ChannelSuccessRatePoint>> response = payServiceClient.getChannelSuccessRate(date, test, merchant);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch channel success rate from pay-service, returning empty list");
            return Collections.emptyList();
        }
        return response.getData();
    }

    public Page<PayConfigChannelDTO> fetchChannelConfigList(String title, Integer nullify, long pageNum, long pageSize) {
        R<Page<PayConfigChannelDTO>> response = payServiceClient.getChannelConfigList(title, nullify, pageNum, pageSize);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch channel config list from pay-service, returning empty page");
            Page<PayConfigChannelDTO> fallback = new Page<>(pageNum, pageSize, 0);
            fallback.setRecords(Collections.emptyList());
            return fallback;
        }
        return response.getData();
    }

    public void updateChannelConfig(PayConfigChannelUpdateRequest req) {
        payServiceClient.updateChannelConfig(req);
    }

    public void updateChannelNullify(Integer id, Integer nullify) {
        payServiceClient.updateChannelNullify(id, nullify);
    }

    public Page<PayConfigInfoDTO> fetchPayConfigInfoList(String shortCode, long pageNum, long pageSize) {
        R<Page<PayConfigInfoDTO>> response = payServiceClient.getPayConfigInfoList(shortCode, pageNum, pageSize);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch pay config info list from pay-service, returning empty page");
            Page<PayConfigInfoDTO> fallback = new Page<>(pageNum, pageSize, 0);
            fallback.setRecords(Collections.emptyList());
            return fallback;
        }
        return response.getData();
    }

    public void createPayConfigInfo(String title, String url) {
        R<Void> response = payServiceClient.createPayConfigInfo(title, url);
        if (response == null || !R.isSuccess(response.getCode())) {
            throw new IllegalStateException("Failed to create pay config info in pay-service");
        }
    }

    public void updatePayConfigInfo(Integer id, String title, String url) {
        payServiceClient.updatePayConfigInfo(id, title, url);
    }

    public List<PayConfigInfoDTO> fetchPayConfigInfoOptions() {
        R<List<PayConfigInfoDTO>> response = payServiceClient.getPayConfigInfoOptions();
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch pay config info options from pay-service");
            return Collections.emptyList();
        }
        return response.getData();
    }

    public void createPayConfigChannel(PayConfigChannelSaveRequest req) {
        R<Void> response = payServiceClient.createPayConfigChannel(req);
        if (response == null || !R.isSuccess(response.getCode())) {
            throw new IllegalStateException("Failed to create pay config channel in pay-service");
        }
    }

    public void updatePayConfigChannel(PayConfigChannelSaveRequest req) {
        payServiceClient.updatePayConfigChannel(req);
    }

    public BigDecimal fetchOrderSumByChannel(Long channelId, Date startTime) {
        R<BigDecimal> response = payServiceClient.getOrderSumByChannel(channelId, startTime.getTime());
        if (response == null || !R.isSuccess(response.getCode())) {
            log.warn("Failed to fetch order sum for channelId={}", channelId);
            return BigDecimal.ZERO;
        }
        return response.getData() != null ? response.getData() : BigDecimal.ZERO;
    }

    public String fetchChannelTitleById(Long channelId) {
        R<String> response = payServiceClient.getChannelTitleById(channelId);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch channel title for channelId={}", channelId);
            return "";
        }
        return response.getData();
    }

    public void updateOrderOnlineId(Long channelId, Date startTime, Long billId) {
        try {
            payServiceClient.updateOrderOnlineId(channelId, startTime.getTime(), billId);
        } catch (Exception e) {
            log.error("Failed to update order online_id for channelId={} billId={}", channelId, billId, e);
        }
    }

    public void manualCallback(String orderId) {
        R<Void> response = payServiceClient.manualCallback(orderId);
        if (response == null || !R.isSuccess(response.getCode())) {
            String msg = response != null ? response.getMsg() : "pay-service unavailable";
            throw new IllegalStateException(msg);
        }
    }

    public List<BillOrderDetailDTO> fetchOrderDetailsByBillId(Long billId) {
        R<List<BillOrderDetailDTO>> response = payServiceClient.getOrderDetailsByBillId(billId);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch order details by billId={} from pay-service", billId);
            return Collections.emptyList();
        }
        return response.getData();
    }

    public List<BillOrderIdsDTO> fetchOrderIdsByBillIds(List<Long> billIds) {
        if (billIds == null || billIds.isEmpty()) return Collections.emptyList();
        R<List<BillOrderIdsDTO>> response = payServiceClient.getOrderIdsByBillIds(billIds);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch order ids by bill ids from pay-service");
            return Collections.emptyList();
        }
        return response.getData();
    }

    public Page<OrderBuildErrorDTO> fetchOrderBuildErrorList(
            String mdcId, String errorText, Integer payConfigId, String appId,
            long pageNum, long pageSize) {
        R<Page<OrderBuildErrorDTO>> response =
                payServiceClient.getOrderBuildErrorList(mdcId, errorText, payConfigId, appId, pageNum, pageSize);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch order build error list from pay-service, returning empty page");
            Page<OrderBuildErrorDTO> fallback = new Page<>(pageNum, pageSize, 0);
            fallback.setRecords(Collections.emptyList());
            return fallback;
        }
        return response.getData();
    }

    public Page<PayinOrderVO> fetchChannelStats(
            Long id, String otherOrderId,
            String createStartTime, String createEndTime,
            String payStartTime, String payEndTime,
            Long channelId, Integer status, String account,
            long pageNum, long pageSize) {
        R<Page<PayinOrderVO>> response = payServiceClient.getChannelStats(
                id, otherOrderId, createStartTime, createEndTime,
                payStartTime, payEndTime, channelId, status, account, pageNum, pageSize);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch channel stats from pay-service, returning empty page");
            Page<PayinOrderVO> fallback = new Page<>(pageNum, pageSize, 0);
            fallback.setRecords(Collections.emptyList());
            return fallback;
        }
        return response.getData();
    }

    public Page<PayinOrderVO> fetchChannelStatsPayout(
            Long id, String otherOrderId,
            String createStartTime, String createEndTime,
            String payStartTime, String payEndTime,
            Long channelId, Integer status, String account,
            long pageNum, long pageSize) {
        R<Page<PayinOrderVO>> response = payServiceClient.getChannelStatsPayout(
                id, otherOrderId, createStartTime, createEndTime,
                payStartTime, payEndTime, channelId, status, account, pageNum, pageSize);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch payout channel stats from pay-service, returning empty page");
            Page<PayinOrderVO> fallback = new Page<>(pageNum, pageSize, 0);
            fallback.setRecords(Collections.emptyList());
            return fallback;
        }
        return response.getData();
    }

    public List<BoardChannelDTO> fetchAllChannelOptions() {
        R<List<BoardChannelDTO>> response = payServiceClient.getAllChannelOptions();
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch all channel options from pay-service");
            return Collections.emptyList();
        }
        return response.getData();
    }

    // ── Merchant profile methods ──────────────────────────────────

    public List<MerchantProfileDTO> fetchMerchantProfiles() {
        R<List<MerchantProfileDTO>> response = payServiceClient.getMerchantProfiles();
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch merchant profiles from pay-service, fallback to empty list");
            return Collections.emptyList();
        }
        return response.getData();
    }

    public Page<MerchantProfileDTO> fetchMerchantPage(String keyword, Integer status,
            long pageNum, long pageSize) {
        R<Page<MerchantProfileDTO>> response =
                payServiceClient.getMerchantPage(keyword, status, pageNum, pageSize);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch merchant page from pay-service, returning empty page");
            Page<MerchantProfileDTO> fallback = new Page<>(pageNum, pageSize, 0);
            fallback.setRecords(Collections.emptyList());
            return fallback;
        }
        return response.getData();
    }

    public MerchantProfileDTO fetchMerchantDetail(Integer platformId) {
        R<MerchantProfileDTO> response = payServiceClient.getMerchantDetail(platformId);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            throw new IllegalArgumentException("商户不存在: " + platformId);
        }
        return response.getData();
    }

    public MerchantProfileDTO createMerchant(String title, Integer status) {
        R<MerchantProfileDTO> response = payServiceClient.createMerchant(title, status);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            throw new IllegalStateException("Failed to create merchant in pay-service");
        }
        return response.getData();
    }

    public void updateMerchant(Integer platformId, String title, Integer status) {
        payServiceClient.updateMerchant(platformId, title, status);
    }

    public void toggleMerchantStatus(Integer platformId, Integer status) {
        payServiceClient.toggleMerchantStatus(platformId, status);
    }

    public void deleteMerchant(Integer platformId) {
        payServiceClient.deleteMerchant(platformId);
    }

    public String resetMerchantKey(Integer platformId) {
        R<String> response = payServiceClient.resetMerchantKey(platformId);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            throw new IllegalStateException("Failed to reset merchant key in pay-service");
        }
        return response.getData();
    }

    // ── Board channel method ──────────────────────────────────────

    public List<Long> fetchActiveChannelIds() {
        R<List<Long>> response = payServiceClient.getActiveChannelIds();
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch active channel ids from pay-service, fallback to empty list");
            return Collections.emptyList();
        }
        return response.getData();
    }

    public List<BoardChannelDTO> fetchChannelsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        R<List<BoardChannelDTO>> response = payServiceClient.getChannelsByIds(ids);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch channels by ids from pay-service, fallback to empty list");
            return Collections.emptyList();
        }
        return response.getData();
    }

    public Page<OrderInfoDTO> fetchMerchantPayinOrders(
            Integer platformId, String orderId, String startDate, String endDate,
            Integer status, long pageNum, long pageSize) {
        R<Page<OrderInfoDTO>> response = payServiceClient.getMerchantPayinOrders(
                platformId, orderId, startDate, endDate, status, pageNum, pageSize);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch merchant payin orders from pay-service for platformId={}", platformId);
            Page<OrderInfoDTO> fallback = new Page<>(pageNum, pageSize, 0);
            fallback.setRecords(Collections.emptyList());
            return fallback;
        }
        return response.getData();
    }

    public OrderInfoDTO fetchOrderInfoById(Long id) {
        R<OrderInfoDTO> response = payServiceClient.getOrderInfoById(id);
        if (response == null || !R.isSuccess(response.getCode())) {
            log.warn("Failed to fetch orderInfo by id={} from pay-service", id);
            return null;
        }
        return response.getData();
    }

    public List<OrderInfoDTO> fetchOrdersByOrderIds(List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return Collections.emptyList();
        R<List<OrderInfoDTO>> response = payServiceClient.getOrdersByOrderIds(orderIds);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch orders by orderIds from pay-service");
            return Collections.emptyList();
        }
        return response.getData();
    }

    public List<OrderCallbackDTO> fetchOrderCallbackList(String orderId) {
        R<List<OrderCallbackDTO>> response = payServiceClient.getOrderCallbackList(orderId);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch order callback list for orderId={} from pay-service", orderId);
            return Collections.emptyList();
        }
        return response.getData();
    }

    public Page<PayChannelPageRowDTO> fetchPayChannelPage(
            Long id, String title, Integer status, String currency, long pageNum, long pageSize) {
        Integer areaType = null;
        if (StringUtils.hasText(currency)) {
            areaType = AreaTypeConstants.MAP.entrySet().stream()
                    .filter(e -> currency.equals(e.getValue().currencyCode))
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(null);
            if (areaType == null) {
                Page<PayChannelPageRowDTO> empty = new Page<>(pageNum, pageSize, 0);
                empty.setRecords(Collections.emptyList());
                return empty;
            }
        }
        R<Page<PayChannelPageRowDTO>> response =
                payServiceClient.getPayChannelPage(id, title, status, areaType, pageNum, pageSize);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch pay channel page from pay-service");
            Page<PayChannelPageRowDTO> fallback = new Page<>(pageNum, pageSize, 0);
            fallback.setRecords(Collections.emptyList());
            return fallback;
        }
        Page<PayChannelPageRowDTO> page = response.getData();
        if (page.getRecords() != null) {
            page.getRecords().forEach(r -> {
                if (r.getAreaType() != null) {
                    r.setCurrency(AreaTypeConstants.currencyCode(r.getAreaType()));
                }
            });
        }
        return page;
    }

    public Page<PayinSummaryRowDTO> fetchPayinSummaryPage(
            String dateType, Integer areaType, long pageNum, long pageSize) {
        String startTime = resolveStartTime(dateType);
        R<Page<PayinSummaryRowDTO>> response =
                payServiceClient.getPayinSummaryPage(startTime, areaType, pageNum, pageSize);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch payin summary page from pay-service");
            Page<PayinSummaryRowDTO> fallback = new Page<>(pageNum, pageSize, 0);
            fallback.setRecords(Collections.emptyList());
            return fallback;
        }
        Page<PayinSummaryRowDTO> page = response.getData();
        List<PayinSummaryRowDTO> records = page.getRecords();
        if (records != null && !records.isEmpty()) {
            Set<Integer> platformIds = records.stream()
                    .map(PayinSummaryRowDTO::getPlatformId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!platformIds.isEmpty()) {
                List<SystemUserAuth> authList = systemUserAuthMapper.selectList(
                        Wrappers.<SystemUserAuth>lambdaQuery()
                                .in(SystemUserAuth::getPlatformId, platformIds)
                                .select(SystemUserAuth::getPlatformId, SystemUserAuth::getAccount));
                Map<Integer, String> accountMap = authList == null ? Collections.emptyMap()
                        : authList.stream()
                                .filter(a -> a.getPlatformId() != null)
                                .collect(Collectors.toMap(SystemUserAuth::getPlatformId,
                                        a -> a.getAccount() != null ? a.getAccount() : "",
                                        (x, y) -> x));
                records.forEach(r -> {
                    if (r.getPlatformId() != null) {
                        r.setMerchant(accountMap.getOrDefault(r.getPlatformId(), ""));
                    }
                });
            }
        }
        return page;
    }

    public Page<PayinSummaryRowDTO> fetchPayoutSummaryPage(
            String dateType, Integer areaType, long pageNum, long pageSize) {
        String startTime = resolveStartTime(dateType);
        R<Page<PayinSummaryRowDTO>> response =
                payServiceClient.getPayoutSummaryPage(startTime, areaType, pageNum, pageSize);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch payout summary page from pay-service");
            Page<PayinSummaryRowDTO> fallback = new Page<>(pageNum, pageSize, 0);
            fallback.setRecords(Collections.emptyList());
            return fallback;
        }
        Page<PayinSummaryRowDTO> page = response.getData();
        List<PayinSummaryRowDTO> records = page.getRecords();
        if (records != null && !records.isEmpty()) {
            Set<Integer> platformIds = records.stream()
                    .map(PayinSummaryRowDTO::getPlatformId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!platformIds.isEmpty()) {
                List<SystemUserAuth> authList = systemUserAuthMapper.selectList(
                        Wrappers.<SystemUserAuth>lambdaQuery()
                                .in(SystemUserAuth::getPlatformId, platformIds)
                                .select(SystemUserAuth::getPlatformId, SystemUserAuth::getAccount));
                Map<Integer, String> accountMap = authList == null ? Collections.emptyMap()
                        : authList.stream()
                                .filter(a -> a.getPlatformId() != null)
                                .collect(Collectors.toMap(SystemUserAuth::getPlatformId,
                                        a -> a.getAccount() != null ? a.getAccount() : "",
                                        (x, y) -> x));
                records.forEach(r -> {
                    if (r.getPlatformId() != null) {
                        r.setMerchant(accountMap.getOrDefault(r.getPlatformId(), ""));
                    }
                });
            }
        }
        return page;
    }

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String resolveStartTime(String dateType) {
        if (!StringUtils.hasText(dateType)) return null;
        LocalDate today = LocalDate.now();
        LocalDate start;
        switch (dateType) {
            case "today":  start = today; break;
            case "week":   start = today.with(DayOfWeek.MONDAY); break;
            case "month":  start = today.withDayOfMonth(1); break;
            case "2m":     start = today.minusMonths(2); break;
            default:       return null;
        }
        return LocalDateTime.of(start, LocalTime.MIDNIGHT).format(DT_FMT);
    }

    private Integer resolveCurrencyToAreaType(String currencyCode) {
        if (!StringUtils.hasText(currencyCode)) return null;
        return AreaTypeConstants.MAP.entrySet().stream()
                .filter(e -> currencyCode.equals(e.getValue().currencyCode))
                .map(java.util.Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public List<ChannelPlatformStatDTO> fetchDailyChannelPlatformStats(String date) {
        R<List<ChannelPlatformStatDTO>> response = payServiceClient.getDailyChannelPlatformStats(date);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch daily channel platform stats from pay-service for date={}", date);
            return Collections.emptyList();
        }
        return response.getData();
    }

    private HomeDashboardMetricsResponse readMetricsFromCache() {
        String json = stringRedisTemplate.opsForValue().get(HOME_METRICS_CACHE_KEY);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, HomeDashboardMetricsResponse.class);
        } catch (IOException e) {
            log.warn("Failed to deserialize cached home metrics", e);
            return null;
        }
    }

    private DashboardSummaryResponse readSummaryFromCache() {
        String json = stringRedisTemplate.opsForValue().get(DASHBOARD_SUMMARY_CACHE_KEY);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, DashboardSummaryResponse.class);
        } catch (IOException e) {
            log.warn("Failed to deserialize cached dashboard summary", e);
            return null;
        }
    }

    private void cacheMetrics(HomeDashboardMetricsResponse metrics) {
        try {
            String payload = objectMapper.writeValueAsString(metrics);
            stringRedisTemplate.opsForValue().set(HOME_METRICS_CACHE_KEY, payload, HOME_METRICS_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize home metrics for cache", e);
        }
    }

    private void cacheSummary(DashboardSummaryResponse summary) {
        try {
            String payload = objectMapper.writeValueAsString(summary);
            stringRedisTemplate.opsForValue().set(DASHBOARD_SUMMARY_CACHE_KEY, payload, DASHBOARD_SUMMARY_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize dashboard summary for cache", e);
        }
    }
}
