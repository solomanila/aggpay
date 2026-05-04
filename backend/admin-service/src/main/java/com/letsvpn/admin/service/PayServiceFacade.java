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
import com.letsvpn.common.core.dto.PayConfigChannelDTO;
import com.letsvpn.common.core.dto.PayConfigChannelUpdateRequest;
import com.letsvpn.common.core.dto.PayConfigInfoDTO;
import com.letsvpn.common.core.response.R;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
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

    public List<ChannelSuccessRatePoint> fetchChannelSuccessRate(String date) {
        R<List<ChannelSuccessRatePoint>> response = payServiceClient.getChannelSuccessRate(date);
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

    public Page<OrderInfoDTO> fetchChannelStats(String period, Integer payConfigId, long pageNum, long pageSize) {
        R<Page<OrderInfoDTO>> response =
                payServiceClient.getChannelStats(period, payConfigId, pageNum, pageSize);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            log.warn("Failed to fetch channel stats from pay-service, returning empty page");
            Page<OrderInfoDTO> fallback = new Page<>(pageNum, pageSize, 0);
            fallback.setRecords(Collections.emptyList());
            return fallback;
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
