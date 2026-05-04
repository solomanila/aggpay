package com.letsvpn.pay.service.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.common.core.dto.BoardChannelDTO;
import com.letsvpn.common.core.dto.ChannelOpenRateRow;
import com.letsvpn.common.core.dto.ChannelSuccessRatePoint;
import com.letsvpn.common.core.dto.DashboardAmountMetric;
import com.letsvpn.common.core.dto.DashboardCountMetric;
import com.letsvpn.common.core.dto.DashboardSuccessRateMetric;
import com.letsvpn.common.core.dto.DashboardSummaryResponse;
import com.letsvpn.common.core.dto.HomeDashboardMetricsResponse;
import com.letsvpn.common.core.dto.MerchantPlatformInfoDTO;
import com.letsvpn.common.core.dto.MerchantProfileDTO;
import com.letsvpn.common.core.dto.OrderBuildErrorDTO;
import com.letsvpn.common.core.dto.OrderInfoDTO;
import com.letsvpn.common.core.dto.PayConfigChannelDTO;
import com.letsvpn.common.core.dto.PayConfigChannelUpdateRequest;
import com.letsvpn.common.core.dto.PayConfigInfoDTO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.letsvpn.pay.entity.MerchantInfo;
import com.letsvpn.pay.entity.OrderBuildError;
import com.letsvpn.pay.entity.OrderInfo;
import com.letsvpn.pay.entity.PayConfigChannel;
import com.letsvpn.pay.entity.PayConfigInfo;
import com.letsvpn.pay.entity.PayPlatformInfo;
import com.letsvpn.pay.mapper.DashboardSummaryMapper;
import com.letsvpn.pay.mapper.MerchantInfoMapper;
import com.letsvpn.pay.mapper.OrderBuildErrorMapper;
import com.letsvpn.pay.mapper.OrderInfoMapper;
import com.letsvpn.pay.mapper.PayConfigChannelMapper;
import com.letsvpn.pay.mapper.PayConfigInfoMapper;
import com.letsvpn.pay.mapper.PayPlatformInfoMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 提供首页看板需要的基础指标。
 */
@Service
@RequiredArgsConstructor
public class DashboardMetricsService {

    private static final String DEFAULT_MINUTE_SLA = "99.95%";

    private final PayConfigInfoMapper payConfigInfoMapper;
    private final DashboardSummaryMapper dashboardSummaryMapper;
    private final MerchantInfoMapper merchantInfoMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderBuildErrorMapper orderBuildErrorMapper;
    private final PayPlatformInfoMapper payPlatformInfoMapper;
    private final PayConfigChannelMapper payConfigChannelMapper;
    private final com.letsvpn.pay.mapper.ext.ExtChannelProfitStatMapper extChannelProfitStatMapper;

    public HomeDashboardMetricsResponse getHomeMetrics() {
        List<Integer> rawAreas = payConfigInfoMapper.selectDistinctAreaTypes();
        List<Integer> operatingAreas = rawAreas == null ? Collections.emptyList()
                : rawAreas.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Long activeChannels = payConfigInfoMapper.countActiveChannels();

        HomeDashboardMetricsResponse response = new HomeDashboardMetricsResponse();
        response.setOperatingCountries(operatingAreas);
        response.setActiveChannelCount(activeChannels == null ? 0L : activeChannels);
        response.setMinuteLevelSla(DEFAULT_MINUTE_SLA);
        return response;
    }

    public DashboardSummaryResponse getDashboardSummary() {
        DashboardSummaryResponse response = new DashboardSummaryResponse();

        DashboardAmountMetric amountMetric = dashboardSummaryMapper.selectTransactionAmount();
        if (amountMetric == null) {
            amountMetric = new DashboardAmountMetric();
        }
        DashboardCountMetric countMetric = dashboardSummaryMapper.selectTransactionCount();
        if (countMetric == null) {
            countMetric = new DashboardCountMetric();
        }
        DashboardSuccessRateMetric successMetric = dashboardSummaryMapper.selectSuccessRateWindow();
        if (successMetric == null) {
            successMetric = new DashboardSuccessRateMetric();
        }

        response.setTransactionAmount(amountMetric);
        response.setTransactionCount(countMetric);
        response.setSuccessRate(successMetric);
        return response;
    }

    public List<String> listMerchantAppIds() {
        List<String> appIds = merchantInfoMapper.selectAllDistinctAppIds();
        if (appIds == null) {
            return Collections.emptyList();
        }
        return appIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<Long> listPayConfigIds() {
        List<Long> ids = payConfigInfoMapper.selectAllIds();
        if (ids == null) {
            return Collections.emptyList();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    public Page<OrderInfoDTO> getChannelStats(String period, Integer payConfigId, long pageNum, long pageSize) {
        long pageIndex = Math.max(pageNum, 1L);
        long size = Math.max(1L, Math.min(pageSize, 200L));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = resolveStartTime(period, now);
        LocalDateTime endTime = resolveEndTime(period, now);

        LambdaQueryWrapper<OrderInfo> wrapper = Wrappers.<OrderInfo>lambdaQuery()
                .orderByDesc(OrderInfo::getPayTime);
        if (payConfigId != null) {
            wrapper.eq(OrderInfo::getPayConfigId, payConfigId);
        }
        if (startTime != null) {
            wrapper.ge(OrderInfo::getPayTime, toDate(startTime));
        }
        if (endTime != null) {
            wrapper.lt(OrderInfo::getPayTime, toDate(endTime));
        }

        Page<OrderInfo> pageResult = orderInfoMapper.selectPage(new Page<>(pageIndex, size), wrapper);
        List<OrderInfoDTO> records = pageResult.getRecords().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        Page<OrderInfoDTO> dtoPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        dtoPage.setRecords(records);
        return dtoPage;
    }

    private LocalDateTime resolveStartTime(String period, LocalDateTime now) {
        String normalized = normalizePeriod(period);
        switch (normalized) {
            case "2w":
                return now.minusWeeks(2);
            case "1w":
                return now.minusWeeks(1);
            default:
                LocalDate today = LocalDate.now();
                return LocalDateTime.of(today, LocalTime.MIN);
        }
    }

    private LocalDateTime resolveEndTime(String period, LocalDateTime now) {
        String normalized = normalizePeriod(period);
        if ("today".equals(normalized)) {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            return LocalDateTime.of(tomorrow, LocalTime.MIN);
        }
        return now;
    }

    private String normalizePeriod(String period) {
        if (period == null || period.isBlank()) {
            return "today";
        }
        String value = period.trim().toLowerCase(Locale.ROOT);
        if (value.contains("two") || value.contains("2") || value.contains("二")) {
            return "2w";
        }
        if (value.contains("one") || value.contains("1") || value.contains("一")) {
            return "1w";
        }
        return "today";
    }

    private Date toDate(LocalDateTime time) {
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }

    public List<ChannelOpenRateRow> getChannelOpenRate(int minutes) {
        int safeMinutes = Math.max(1, Math.min(minutes, 1440));
        List<ChannelOpenRateRow> result = dashboardSummaryMapper.selectChannelOpenRate(safeMinutes);
        return result == null ? Collections.emptyList() : result;
    }

    public List<ChannelSuccessRatePoint> getChannelSuccessRate(String date) {
        LocalDateTime startTime;
        LocalDateTime endTime;
        if ("yesterday".equalsIgnoreCase(date)) {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            startTime = LocalDateTime.of(yesterday, LocalTime.MIN);
            endTime = LocalDateTime.of(yesterday.plusDays(1), LocalTime.MIN);
        } else {
            LocalDate today = LocalDate.now();
            startTime = LocalDateTime.of(today, LocalTime.MIN);
            endTime = LocalDateTime.of(today.plusDays(1), LocalTime.MIN);
        }
        List<ChannelSuccessRatePoint> result = dashboardSummaryMapper.selectChannelSuccessRate(
                toDate(startTime), toDate(endTime));
        return result == null ? Collections.emptyList() : result;
    }

    public Page<PayConfigInfoDTO> getPayConfigInfoList(String shortCode, long pageNum, long pageSize) {
        long pageIndex = Math.max(pageNum, 1L);
        long size = Math.max(1L, Math.min(pageSize, 200L));

        LambdaQueryWrapper<PayConfigInfo> wrapper = Wrappers.<PayConfigInfo>lambdaQuery()
                .select(PayConfigInfo::getId, PayConfigInfo::getShortCode, PayConfigInfo::getUrl,
                        PayConfigInfo::getReqDomain, PayConfigInfo::getRemark, PayConfigInfo::getCreateTime)
                .orderByDesc(PayConfigInfo::getCreateTime);

        if (shortCode != null && !shortCode.isBlank()) {
            wrapper.like(PayConfigInfo::getShortCode, shortCode.trim());
        }

        Page<PayConfigInfo> pageResult = payConfigInfoMapper.selectPage(new Page<>(pageIndex, size), wrapper);
        List<PayConfigInfoDTO> records = pageResult.getRecords().stream()
                .map(this::convertPayConfigInfoToDto)
                .collect(Collectors.toList());
        Page<PayConfigInfoDTO> dtoPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        dtoPage.setRecords(records);
        return dtoPage;
    }

    private OrderInfoDTO convertToDto(OrderInfo orderInfo) {
        OrderInfoDTO dto = new OrderInfoDTO();
        BeanUtils.copyProperties(orderInfo, dto);
        return dto;
    }

    public Page<PayConfigChannelDTO> getChannelConfigList(String title, Integer nullify,
            long pageNum, long pageSize) {
        long pageIndex = Math.max(pageNum, 1L);
        long size = Math.max(1L, Math.min(pageSize, 200L));

        LambdaQueryWrapper<PayConfigInfo> wrapper = Wrappers.<PayConfigInfo>lambdaQuery()
                .select(PayConfigInfo::getId, PayConfigInfo::getAreaType, PayConfigInfo::getTitle,
                        PayConfigInfo::getUrl, PayConfigInfo::getRemark, PayConfigInfo::getNullify,
                        PayConfigInfo::getCreateTime, PayConfigInfo::getThirdService,
                        PayConfigInfo::getShortCode, PayConfigInfo::getReqParam)
                .orderByDesc(PayConfigInfo::getCreateTime);

        if (title != null && !title.isBlank()) {
            wrapper.like(PayConfigInfo::getTitle, title.trim());
        }
        if (nullify != null) {
            wrapper.eq(PayConfigInfo::getNullify, nullify);
        }

        Page<PayConfigInfo> pageResult = payConfigInfoMapper.selectPage(new Page<>(pageIndex, size), wrapper);

        List<Integer> ids = pageResult.getRecords().stream()
                .map(PayConfigInfo::getId).filter(Objects::nonNull).collect(Collectors.toList());
        Map<Integer, MerchantInfo> merchantMap = Collections.emptyMap();
        if (!ids.isEmpty()) {
            List<MerchantInfo> merchants = merchantInfoMapper.selectList(
                    Wrappers.<MerchantInfo>lambdaQuery().in(MerchantInfo::getPayConfigId, ids));
            if (merchants != null) {
                merchantMap = merchants.stream()
                        .collect(Collectors.toMap(MerchantInfo::getPayConfigId, m -> m, (a, b) -> a));
            }
        }

        final Map<Integer, MerchantInfo> finalMerchantMap = merchantMap;
        List<PayConfigChannelDTO> records = pageResult.getRecords().stream()
                .map(info -> {
                    PayConfigChannelDTO dto = convertToChannelDto(info);
                    MerchantInfo m = finalMerchantMap.get(info.getId());
                    if (m != null) {
                        dto.setAppId(m.getAppId());
                        dto.setPrivateKey(m.getPrivateKey());
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        Page<PayConfigChannelDTO> dtoPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        dtoPage.setRecords(records);
        return dtoPage;
    }

    public void updateChannelNullify(Integer id, Integer nullify) {
        if (id == null || nullify == null) {
            throw new IllegalArgumentException("id and nullify are required");
        }
        payConfigInfoMapper.update(null, new LambdaUpdateWrapper<PayConfigInfo>()
                .set(PayConfigInfo::getNullify, nullify)
                .eq(PayConfigInfo::getId, id));
    }

    public void updateChannelConfig(PayConfigChannelUpdateRequest req) {
        if (req.getId() == null) {
            throw new IllegalArgumentException("id is required");
        }
        PayConfigInfo info = new PayConfigInfo();
        info.setId(req.getId());
        if (req.getTitle() != null) info.setTitle(req.getTitle());
        if (req.getAreaType() != null) info.setAreaType(req.getAreaType());
        if (req.getRemark() != null) info.setRemark(req.getRemark());
        if (req.getNullify() != null) info.setNullify(req.getNullify());
        if (req.getThirdService() != null) info.setThirdService(req.getThirdService());
        if (req.getShortCode() != null) info.setShortCode(req.getShortCode());
        if (req.getReqParam() != null) info.setReqParam(req.getReqParam());
        payConfigInfoMapper.updateById(info);

        if (req.getAppId() != null || req.getPrivateKey() != null) {
            MerchantInfo existing = merchantInfoMapper.selectOne(
                    Wrappers.<MerchantInfo>lambdaQuery().eq(MerchantInfo::getPayConfigId, req.getId()));
            if (existing != null) {
                LambdaUpdateWrapper<MerchantInfo> mWrapper = new LambdaUpdateWrapper<MerchantInfo>()
                        .eq(MerchantInfo::getPayConfigId, req.getId());
                if (req.getAppId() != null) mWrapper.set(MerchantInfo::getAppId, req.getAppId());
                if (req.getPrivateKey() != null) mWrapper.set(MerchantInfo::getPrivateKey, req.getPrivateKey());
                merchantInfoMapper.update(null, mWrapper);
            }
        }
    }

    private PayConfigInfoDTO convertPayConfigInfoToDto(PayConfigInfo info) {
        PayConfigInfoDTO dto = new PayConfigInfoDTO();
        BeanUtils.copyProperties(info, dto);
        return dto;
    }

    // ── Merchant profile methods (for admin-service Feign) ─────────────

    public List<MerchantProfileDTO> listMerchantProfiles() {
        List<PayPlatformInfo> list = payPlatformInfoMapper.selectList(null);
        if (list == null) return Collections.emptyList();
        return list.stream().map(this::toMerchantProfileDTO).collect(Collectors.toList());
    }

    public Page<MerchantProfileDTO> getMerchantPage(String keyword, Integer status,
            long pageNum, long pageSize) {
        long pageIndex = Math.max(pageNum, 1L);
        long size = Math.max(1L, Math.min(pageSize, 200L));
        LambdaQueryWrapper<PayPlatformInfo> qw = Wrappers.<PayPlatformInfo>lambdaQuery()
                .like(StringUtils.hasText(keyword), PayPlatformInfo::getTitle, keyword)
                .eq(status != null, PayPlatformInfo::getNullify, status != null ? (status == 1 ? 0 : 1) : null)
                .orderByDesc(PayPlatformInfo::getPlatformId);
        Page<PayPlatformInfo> page = payPlatformInfoMapper.selectPage(new Page<>(pageIndex, size), qw);
        List<MerchantProfileDTO> records = page.getRecords().stream()
                .map(this::toMerchantProfileDTO).collect(Collectors.toList());
        Page<MerchantProfileDTO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(records);
        return result;
    }

    public MerchantProfileDTO getMerchantDetail(Integer platformId) {
        PayPlatformInfo info = payPlatformInfoMapper.selectById(platformId);
        if (info == null) throw new IllegalArgumentException("商户不存在: " + platformId);
        return toMerchantProfileDTO(info);
    }

    public MerchantProfileDTO createMerchant(String title, Integer status) {
        PayPlatformInfo info = new PayPlatformInfo();
        info.setTitle(title);
        info.setNullify(status != null && status == 0 ? 1 : 0);
        info.setPlatformNo(String.valueOf(System.currentTimeMillis()) + (int)(Math.random() * 10000));
        info.setSecretKey(UUID.randomUUID().toString().replace("-", ""));
        payPlatformInfoMapper.insert(info);
        return toMerchantProfileDTO(info);
    }

    public void updateMerchant(Integer platformId, String title, Integer status) {
        PayPlatformInfo info = payPlatformInfoMapper.selectById(platformId);
        if (info == null) throw new IllegalArgumentException("商户不存在: " + platformId);
        if (StringUtils.hasText(title)) info.setTitle(title);
        if (status != null) info.setNullify(status == 1 ? 0 : 1);
        payPlatformInfoMapper.updateById(info);
    }

    public void toggleMerchantStatus(Integer platformId, Integer status) {
        int nullify = (status != null && status == 1) ? 0 : 1;
        payPlatformInfoMapper.update(null, new LambdaUpdateWrapper<PayPlatformInfo>()
                .set(PayPlatformInfo::getNullify, nullify)
                .eq(PayPlatformInfo::getPlatformId, platformId));
    }

    public void deleteMerchant(Integer platformId) {
        payPlatformInfoMapper.deleteById(platformId);
    }

    public String resetMerchantKey(Integer platformId) {
        String newKey = UUID.randomUUID().toString().replace("-", "");
        payPlatformInfoMapper.update(null, new LambdaUpdateWrapper<PayPlatformInfo>()
                .set(PayPlatformInfo::getSecretKey, newKey)
                .eq(PayPlatformInfo::getPlatformId, platformId));
        return newKey;
    }

    // ── Board channel methods ──────────────────────────────────────

    public List<Long> getActiveChannelIds() {
        List<PayConfigChannel> list = payConfigChannelMapper.selectList(
                Wrappers.<PayConfigChannel>lambdaQuery()
                        .eq(PayConfigChannel::getStatus, 0)
                        .select(PayConfigChannel::getId));
        if (list == null) return Collections.emptyList();
        return list.stream().map(PayConfigChannel::getId).collect(Collectors.toList());
    }

    public List<BoardChannelDTO> getChannelsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        List<PayConfigChannel> channels = payConfigChannelMapper.selectBatchIds(ids);
        if (channels == null || channels.isEmpty()) return Collections.emptyList();
        Set<Integer> payConfigIds = channels.stream()
                .map(PayConfigChannel::getPayConfigId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Integer, String> titleMap = Collections.emptyMap();
        if (!payConfigIds.isEmpty()) {
            List<PayConfigInfo> infos = payConfigInfoMapper.selectList(
                    Wrappers.<PayConfigInfo>lambdaQuery()
                            .select(PayConfigInfo::getId, PayConfigInfo::getTitle)
                            .in(PayConfigInfo::getId, payConfigIds));
            if (infos != null) {
                titleMap = infos.stream()
                        .collect(Collectors.toMap(PayConfigInfo::getId,
                                i -> i.getTitle() != null ? i.getTitle() : "", (a, b) -> a));
            }
        }
        Map<Integer, String> finalTitleMap = titleMap;
        return channels.stream().map(c -> {
            BoardChannelDTO dto = new BoardChannelDTO();
            dto.setId(c.getId());
            dto.setTitle(c.getTitle());
            dto.setPayConfigId(c.getPayConfigId());
            dto.setPayConfigTitle(finalTitleMap.getOrDefault(c.getPayConfigId(), ""));
            return dto;
        }).sorted(java.util.Comparator.comparing(BoardChannelDTO::getId)).collect(Collectors.toList());
    }

    private MerchantProfileDTO toMerchantProfileDTO(PayPlatformInfo info) {
        MerchantProfileDTO dto = new MerchantProfileDTO();
        dto.setPlatformId(info.getPlatformId());
        dto.setPlatformNo(info.getPlatformNo());
        dto.setTitle(info.getTitle());
        dto.setSecretKey(info.getSecretKey());
        dto.setNullify(info.getNullify());
        dto.setCreateTime(info.getCreateTime());
        return dto;
    }

    private PayConfigChannelDTO convertToChannelDto(PayConfigInfo info) {
        PayConfigChannelDTO dto = new PayConfigChannelDTO();
        dto.setId(info.getId());
        dto.setAreaType(info.getAreaType());
        dto.setTitle(info.getTitle());
        dto.setUrl(info.getUrl());
        dto.setRemark(info.getRemark());
        dto.setNullify(info.getNullify());
        dto.setCreateTime(info.getCreateTime());
        dto.setThirdService(info.getThirdService());
        dto.setShortCode(info.getShortCode());
        dto.setReqParam(info.getReqParam());
        return dto;
    }

    public Page<OrderBuildErrorDTO> getOrderBuildErrorList(
            String mdcId, String errorText, Integer payConfigId, String appId,
            long pageNum, long pageSize) {
        long pageIndex = Math.max(pageNum, 1L);
        long size = Math.max(1L, Math.min(pageSize, 200L));

        // Resolve payConfigIds from appId filter
        List<Integer> appIdPayConfigIds = null;
        if (appId != null && !appId.isBlank()) {
            List<MerchantInfo> matched = merchantInfoMapper.selectList(
                    Wrappers.<MerchantInfo>lambdaQuery().eq(MerchantInfo::getAppId, appId.trim()));
            appIdPayConfigIds = matched == null ? Collections.emptyList()
                    : matched.stream().map(MerchantInfo::getPayConfigId)
                            .filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (appIdPayConfigIds.isEmpty()) {
                Page<OrderBuildErrorDTO> empty = new Page<>(pageIndex, size, 0);
                empty.setRecords(Collections.emptyList());
                return empty;
            }
        }

        LambdaQueryWrapper<OrderBuildError> wrapper = Wrappers.<OrderBuildError>lambdaQuery()
                .select(OrderBuildError::getId, OrderBuildError::getMdcId,
                        OrderBuildError::getPayConfigId, OrderBuildError::getErrorText,
                        OrderBuildError::getCreateTime)
                .orderByDesc(OrderBuildError::getCreateTime);

        if (mdcId != null && !mdcId.isBlank()) {
            wrapper.like(OrderBuildError::getMdcId, mdcId.trim());
        }
        if (errorText != null && !errorText.isBlank()) {
            wrapper.like(OrderBuildError::getErrorText, errorText.trim());
        }
        if (payConfigId != null) {
            wrapper.eq(OrderBuildError::getPayConfigId, payConfigId);
        }
        if (appIdPayConfigIds != null) {
            wrapper.in(OrderBuildError::getPayConfigId, appIdPayConfigIds);
        }

        Page<OrderBuildError> pageResult = orderBuildErrorMapper.selectPage(
                new Page<>(pageIndex, size), wrapper);

        List<Integer> configIds = pageResult.getRecords().stream()
                .map(OrderBuildError::getPayConfigId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());

        Map<Integer, String> titleMap = new HashMap<>();
        Map<Integer, String> appIdMap = new HashMap<>();
        if (!configIds.isEmpty()) {
            List<PayConfigInfo> configs = payConfigInfoMapper.selectList(
                    Wrappers.<PayConfigInfo>lambdaQuery()
                            .select(PayConfigInfo::getId, PayConfigInfo::getTitle)
                            .in(PayConfigInfo::getId, configIds));
            if (configs != null) {
                configs.forEach(c -> titleMap.put(c.getId(),
                        c.getTitle() != null ? c.getTitle() : ""));
            }
            List<MerchantInfo> merchants = merchantInfoMapper.selectList(
                    Wrappers.<MerchantInfo>lambdaQuery()
                            .select(MerchantInfo::getPayConfigId, MerchantInfo::getAppId)
                            .in(MerchantInfo::getPayConfigId, configIds));
            if (merchants != null) {
                merchants.forEach(m -> appIdMap.putIfAbsent(m.getPayConfigId(),
                        m.getAppId() != null ? m.getAppId() : ""));
            }
        }

        List<OrderBuildErrorDTO> records = pageResult.getRecords().stream()
                .map(err -> {
                    OrderBuildErrorDTO dto = new OrderBuildErrorDTO();
                    dto.setId(err.getId());
                    dto.setMdcId(err.getMdcId());
                    dto.setErrorText(err.getErrorText());
                    dto.setCreateTime(err.getCreateTime());
                    dto.setTitle(titleMap.getOrDefault(err.getPayConfigId(), ""));
                    dto.setAppId(appIdMap.getOrDefault(err.getPayConfigId(), ""));
                    return dto;
                })
                .collect(Collectors.toList());

        Page<OrderBuildErrorDTO> dtoPage = new Page<>(
                pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        dtoPage.setRecords(records);
        return dtoPage;
    }

    public List<com.letsvpn.common.core.dto.ChannelPlatformStatDTO> getDailyChannelPlatformStats(String date) {
        return extChannelProfitStatMapper.queryDailyStatsByPlatform(date);
    }

    public java.math.BigDecimal getMerchantTotalIncome(Integer platformId) {
        java.math.BigDecimal result = orderInfoMapper.sumTotalIncomeByPlatform(platformId);
        return result != null ? result : java.math.BigDecimal.ZERO;
    }

    public MerchantPlatformInfoDTO getMerchantPlatformInfo(Integer platformId) {
        PayPlatformInfo info = payPlatformInfoMapper.selectById(platformId);
        if (info == null) {
            return new MerchantPlatformInfoDTO();
        }
        MerchantPlatformInfoDTO dto = new MerchantPlatformInfoDTO();
        dto.setPlatformNo(info.getPlatformNo());
        dto.setSecretKey(info.getSecretKey());
        return dto;
    }

    public Page<OrderInfoDTO> getMerchantPayinOrders(
            Integer platformId, String orderId, String startDate, String endDate,
            Integer status, long pageNum, long pageSize) {
        long pageIndex = Math.max(pageNum, 1L);
        long size = Math.max(1L, Math.min(pageSize, 200L));

        LambdaQueryWrapper<OrderInfo> wrapper = Wrappers.<OrderInfo>lambdaQuery()
                .eq(OrderInfo::getPlatformId, platformId)
                .orderByDesc(OrderInfo::getCreateTime);

        if (orderId != null && !orderId.isBlank()) {
            wrapper.and(w -> w.eq(OrderInfo::getOrderId, orderId.trim())
                    .or().eq(OrderInfo::getFrontId, orderId.trim()));
        }
        if (status != null) {
            wrapper.eq(OrderInfo::getStatus, status);
        }
        if (startDate != null && !startDate.isBlank()) {
            try {
                java.time.LocalDate start = java.time.LocalDate.parse(startDate);
                wrapper.ge(OrderInfo::getCreateTime,
                        toDate(LocalDateTime.of(start, LocalTime.MIN)));
            } catch (Exception ignored) {}
        }
        if (endDate != null && !endDate.isBlank()) {
            try {
                java.time.LocalDate end = java.time.LocalDate.parse(endDate);
                wrapper.lt(OrderInfo::getCreateTime,
                        toDate(LocalDateTime.of(end.plusDays(1), LocalTime.MIN)));
            } catch (Exception ignored) {}
        }

        Page<OrderInfo> pageResult = orderInfoMapper.selectPage(new Page<>(pageIndex, size), wrapper);
        List<OrderInfoDTO> records = pageResult.getRecords().stream()
                .map(this::convertToDto).collect(Collectors.toList());
        Page<OrderInfoDTO> dtoPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        dtoPage.setRecords(records);
        return dtoPage;
    }
}
