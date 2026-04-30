package com.letsvpn.admin.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.common.core.dto.BoardChannelDTO;
import com.letsvpn.common.core.dto.ChannelOpenRateRow;
import com.letsvpn.common.core.dto.ChannelPlatformStatDTO;
import com.letsvpn.common.core.dto.ChannelSuccessRatePoint;
import com.letsvpn.common.core.dto.OrderBuildErrorDTO;
import com.letsvpn.common.core.dto.DashboardAmountMetric;
import com.letsvpn.common.core.dto.DashboardCountMetric;
import com.letsvpn.common.core.dto.DashboardSuccessRateMetric;
import com.letsvpn.common.core.dto.DashboardSummaryResponse;
import com.letsvpn.common.core.dto.HomeDashboardMetricsResponse;
import com.letsvpn.common.core.dto.MerchantProfileDTO;
import com.letsvpn.common.core.dto.OrderInfoDTO;
import com.letsvpn.common.core.dto.PayConfigChannelDTO;
import com.letsvpn.common.core.dto.PayConfigChannelUpdateRequest;
import com.letsvpn.common.core.dto.PayConfigInfoDTO;
import com.letsvpn.common.core.response.R;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * pay-service Feign 调用的统一兜底逻辑，避免链路级联失败。
 */
@Component
@Slf4j
public class PayServiceClientFallbackFactory implements FallbackFactory<PayServiceClient> {

    @Override
    public PayServiceClient create(Throwable cause) {
        log.warn("pay-service feign call failed, entering fallback", cause);
        return new PayServiceClient() {
            @Override
            public String ping(String echo) {
                return "pay-service unavailable";
            }

            @Override
            public R<HomeDashboardMetricsResponse> getHomeMetrics() {
                HomeDashboardMetricsResponse fallback = new HomeDashboardMetricsResponse();
                fallback.setOperatingCountries(Collections.emptyList());
                fallback.setActiveChannelCount(0L);
                fallback.setMinuteLevelSla("99.95%");
                return R.success("fallback", fallback);
            }

            @Override
            public R<DashboardSummaryResponse> getDashboardSummary() {
                DashboardSummaryResponse response = new DashboardSummaryResponse();
                response.setTransactionAmount(new DashboardAmountMetric());
                response.setTransactionCount(new DashboardCountMetric());
                response.setSuccessRate(new DashboardSuccessRateMetric());
                return R.success("fallback", response);
            }

            @Override
            public R<List<String>> getMerchantAppIds() {
                return R.success(Collections.emptyList());
            }

            @Override
            public R<List<Long>> getPayConfigIds() {
                return R.success(Collections.emptyList());
            }

            @Override
            public R<List<ChannelOpenRateRow>> getChannelOpenRate(int minutes) {
                return R.success(Collections.emptyList());
            }

            @Override
            public R<List<ChannelSuccessRatePoint>> getChannelSuccessRate(String date) {
                return R.success(Collections.emptyList());
            }

            @Override
            public R<Page<PayConfigChannelDTO>> getChannelConfigList(
                    String title, Integer nullify, long pageNum, long pageSize) {
                Page<PayConfigChannelDTO> page = new Page<>(pageNum, pageSize, 0);
                page.setRecords(Collections.emptyList());
                return R.success(page);
            }

            @Override
            public R<Void> updateChannelConfig(PayConfigChannelUpdateRequest req) {
                return R.success(null);
            }

            @Override
            public R<Void> updateChannelNullify(Integer id, Integer nullify) {
                return R.success(null);
            }

            @Override
            public R<Page<PayConfigInfoDTO>> getPayConfigInfoList(
                    String shortCode, long pageNum, long pageSize) {
                Page<PayConfigInfoDTO> page = new Page<>(pageNum, pageSize, 0);
                page.setRecords(Collections.emptyList());
                return R.success(page);
            }

            @Override
            public R<Page<OrderBuildErrorDTO>> getOrderBuildErrorList(
                    String mdcId, String errorText, Integer payConfigId, String appId,
                    long pageNum, long pageSize) {
                Page<OrderBuildErrorDTO> page = new Page<>(pageNum, pageSize, 0);
                page.setRecords(Collections.emptyList());
                return R.success(page);
            }

            @Override
            public R<Page<OrderInfoDTO>> getChannelStats(
                    String period, Integer payConfigId, long pageNum, long pageSize) {
                Page<OrderInfoDTO> page = new Page<>(pageNum, pageSize, 0);
                page.setRecords(Collections.emptyList());
                return R.success(page);
            }

            @Override
            public R<List<MerchantProfileDTO>> getMerchantProfiles() {
                return R.success(Collections.emptyList());
            }

            @Override
            public R<Page<MerchantProfileDTO>> getMerchantPage(
                    String keyword, Integer status, long pageNum, long pageSize) {
                Page<MerchantProfileDTO> page = new Page<>(pageNum, pageSize, 0);
                page.setRecords(Collections.emptyList());
                return R.success(page);
            }

            @Override
            public R<MerchantProfileDTO> getMerchantDetail(Integer platformId) {
                return R.success(null);
            }

            @Override
            public R<MerchantProfileDTO> createMerchant(String title, Integer status) {
                return R.success(null);
            }

            @Override
            public R<Void> updateMerchant(Integer platformId, String title, Integer status) {
                return R.success(null);
            }

            @Override
            public R<Void> toggleMerchantStatus(Integer platformId, Integer status) {
                return R.success(null);
            }

            @Override
            public R<Void> deleteMerchant(Integer platformId) {
                return R.success(null);
            }

            @Override
            public R<String> resetMerchantKey(Integer platformId) {
                return R.success(null);
            }

            @Override
            public R<List<Long>> getActiveChannelIds() {
                return R.success(Collections.emptyList());
            }

            @Override
            public R<List<BoardChannelDTO>> getChannelsByIds(List<Long> ids) {
                return R.success(Collections.emptyList());
            }

            @Override
            public R<List<ChannelPlatformStatDTO>> getDailyChannelPlatformStats(String date) {
                return R.success(Collections.emptyList());
            }
        };
    }
}
