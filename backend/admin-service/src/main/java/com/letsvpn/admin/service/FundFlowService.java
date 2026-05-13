package com.letsvpn.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.admin.constant.AreaTypeConstants;
import com.letsvpn.admin.dto.FundFlowRequest;
import com.letsvpn.admin.dto.FundFlowVO;
import com.letsvpn.admin.dto.MerchantOptionVO;
import com.letsvpn.admin.entity.MerchantBalanceLog;
import com.letsvpn.admin.entity.MerchantChannelConfig;
import com.letsvpn.admin.entity.SystemUserAuth;
import com.letsvpn.admin.mapper.MerchantBalanceLogMapper;
import com.letsvpn.admin.mapper.MerchantChannelConfigMapper;
import com.letsvpn.admin.mapper.SystemUserAuthMapper;
import com.letsvpn.common.core.dto.MerchantProfileDTO;
import com.letsvpn.common.core.dto.OrderInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FundFlowService {

    private final MerchantBalanceLogMapper balanceLogMapper;
    private final SystemUserAuthMapper systemUserAuthMapper;
    private final MerchantChannelConfigMapper channelConfigMapper;
    private final PayServiceFacade payServiceFacade;

    public List<MerchantOptionVO> listMerchantOptions() {
        // 从 pay-service 获取 aggpay.pay_platform_info 中的合法 platform_id 集合（跨 schema，Feign 调用）
        List<MerchantProfileDTO> profiles = payServiceFacade.fetchMerchantProfiles();
        Set<Integer> validPlatformIds = profiles.stream()
                .map(MerchantProfileDTO::getPlatformId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (validPlatformIds.isEmpty()) {
            return Collections.emptyList();
        }

        // INNER JOIN 语义：只返回在 pay_platform_info 中存在的 platform_id 对应的账号
        List<SystemUserAuth> users = systemUserAuthMapper.selectList(
                new LambdaQueryWrapper<SystemUserAuth>()
                        .in(SystemUserAuth::getPlatformId, validPlatformIds));
        return users.stream().map(u -> {
            MerchantOptionVO vo = new MerchantOptionVO();
            vo.setPlatformId(u.getPlatformId());
            vo.setAccount(u.getAccount());
            return vo;
        }).collect(Collectors.toList());
    }

    public Page<FundFlowVO> queryPage(FundFlowRequest req) {
        LambdaQueryWrapper<MerchantBalanceLog> wrapper = new LambdaQueryWrapper<>();

        Date startDate = resolveStartDate(req.getDateType());
        if (startDate != null) {
            wrapper.ge(MerchantBalanceLog::getCreatedAt, startDate);
        }

        if (req.getPlatformIds() != null && !req.getPlatformIds().isEmpty()) {
            wrapper.in(MerchantBalanceLog::getPlatformId, req.getPlatformIds());
        }

        if (req.getAreaTypes() != null && !req.getAreaTypes().isEmpty()) {
            List<String> currencies = req.getAreaTypes().stream()
                    .map(AreaTypeConstants::currencyCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!currencies.isEmpty()) {
                wrapper.in(MerchantBalanceLog::getCurrency, currencies);
            }
        }

        if (StringUtils.hasText(req.getRefId())) {
            wrapper.and(w -> w.like(MerchantBalanceLog::getOrderId, req.getRefId())
                    .or()
                    .like(MerchantBalanceLog::getOtherOrderId, req.getRefId()));
        }

        wrapper.orderByDesc(MerchantBalanceLog::getId);

        Page<MerchantBalanceLog> pageResult = balanceLogMapper.selectPage(
                new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        List<MerchantBalanceLog> logs = pageResult.getRecords();

        // Batch fetch order info from pay-service (cross-schema: aggpay.order_info)
        List<String> orderIds = logs.stream()
                .map(MerchantBalanceLog::getOrderId)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        Map<String, OrderInfoDTO> orderMap = Collections.emptyMap();
        if (!orderIds.isEmpty()) {
            List<OrderInfoDTO> orders = payServiceFacade.fetchOrdersByOrderIds(orderIds);
            orderMap = orders.stream()
                    .filter(o -> o.getOrderId() != null)
                    .collect(Collectors.toMap(OrderInfoDTO::getOrderId, o -> o, (a, b) -> a));
        }

        // Merchant account map
        Set<Integer> platformIdSet = logs.stream()
                .map(MerchantBalanceLog::getPlatformId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, String> accountMap = Collections.emptyMap();
        if (!platformIdSet.isEmpty()) {
            List<SystemUserAuth> users = systemUserAuthMapper.selectList(
                    new LambdaQueryWrapper<SystemUserAuth>()
                            .in(SystemUserAuth::getPlatformId, platformIdSet));
            accountMap = users.stream()
                    .filter(u -> u.getPlatformId() != null)
                    .collect(Collectors.toMap(SystemUserAuth::getPlatformId, SystemUserAuth::getAccount, (a, b) -> a));
        }

        // Channel config fee rate map: "platformId_channelId" -> feeRate
        Map<String, BigDecimal> feeRateMap = new HashMap<>();
        for (OrderInfoDTO order : orderMap.values()) {
            if (order.getPlatformId() != null && order.getPayConfigChannelId() != null) {
                String key = order.getPlatformId() + "_" + order.getPayConfigChannelId();
                if (!feeRateMap.containsKey(key)) {
                    MerchantChannelConfig config = channelConfigMapper.selectOne(
                            new LambdaQueryWrapper<MerchantChannelConfig>()
                                    .eq(MerchantChannelConfig::getPlatformId, order.getPlatformId())
                                    .eq(MerchantChannelConfig::getPayConfigChannelId, order.getPayConfigChannelId())
                                    .last("LIMIT 1"));
                    BigDecimal rate = (config != null && config.getFeeRate() != null)
                            ? config.getFeeRate() : BigDecimal.ZERO;
                    feeRateMap.put(key, rate);
                }
            }
        }

        Map<String, OrderInfoDTO> finalOrderMap = orderMap;
        Map<Integer, String> finalAccountMap = accountMap;
        List<FundFlowVO> voList = logs.stream().map(log -> {
            FundFlowVO vo = new FundFlowVO();
            vo.setId(log.getId());
            vo.setMerchant(finalAccountMap.get(log.getPlatformId()));
            vo.setAmount(log.getAmount());
            vo.setRemain(log.getAfterAvailable());
            vo.setCurrency(log.getCurrency());
            vo.setReason(log.getOpType());
            vo.setRefId(log.getOrderId());
            vo.setLocalTime(log.getCreatedAt());
            vo.setOutTradeNo(log.getOtherOrderId());
            vo.setType(isIncrease(log.getOpType()) ? "INCREASE" : "DECREASE");

            if (StringUtils.hasText(log.getOrderId())) {
                OrderInfoDTO order = finalOrderMap.get(log.getOrderId());
                if (order != null) {
                    vo.setOrderAmount(order.getRealAmount());
                    if (order.getRealAmount() != null
                            && order.getPlatformId() != null
                            && order.getPayConfigChannelId() != null) {
                        String key = order.getPlatformId() + "_" + order.getPayConfigChannelId();
                        BigDecimal rate = feeRateMap.getOrDefault(key, BigDecimal.ZERO);
                        vo.setOrderFee(order.getRealAmount().multiply(rate));
                    }
                }
            }
            return vo;
        }).collect(Collectors.toList());

        Page<FundFlowVO> result = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        result.setRecords(voList);
        return result;
    }

    private boolean isIncrease(String opType) {
        return "SETTLEMENT".equals(opType) || "RECHARGE".equals(opType);
    }

    private Date resolveStartDate(String dateType) {
        LocalDate today = LocalDate.now();
        LocalDate start;
        if ("week".equals(dateType)) {
            start = today.with(DayOfWeek.MONDAY);
        } else if ("month".equals(dateType)) {
            start = today.withDayOfMonth(1);
        } else {
            start = today;
        }
        return Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
