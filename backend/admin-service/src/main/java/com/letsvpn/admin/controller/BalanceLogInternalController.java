package com.letsvpn.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.letsvpn.admin.entity.MerchantBalanceLog;
import com.letsvpn.admin.mapper.MerchantBalanceLogMapper;
import com.letsvpn.common.core.dto.OrderCreatedAtDTO;
import com.letsvpn.common.core.response.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/internal/balance-log")
@RequiredArgsConstructor
public class BalanceLogInternalController {

    private final MerchantBalanceLogMapper balanceLogMapper;

    /**
     * 按 order_id 批量查询 merchant_balance_log.created_at
     * 每个 order_id 取 id 最小的那条记录（最早写入时间）
     */
    @PostMapping("/created-at-by-order-ids")
    public R<List<OrderCreatedAtDTO>> getCreatedAtByOrderIds(@RequestBody List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return R.success(Collections.emptyList());
        }
        List<MerchantBalanceLog> logs = balanceLogMapper.selectList(
                new LambdaQueryWrapper<MerchantBalanceLog>()
                        .select(MerchantBalanceLog::getOrderId, MerchantBalanceLog::getId,
                                MerchantBalanceLog::getCreatedAt)
                        .in(MerchantBalanceLog::getOrderId, orderIds)
                        .isNotNull(MerchantBalanceLog::getOrderId));

        // Deduplicate: per order_id keep the record with the smallest id (earliest)
        Map<String, MerchantBalanceLog> earliest = logs.stream()
                .filter(l -> l.getOrderId() != null)
                .collect(Collectors.toMap(
                        MerchantBalanceLog::getOrderId,
                        l -> l,
                        (a, b) -> a.getId() <= b.getId() ? a : b));

        List<OrderCreatedAtDTO> result = earliest.values().stream().map(l -> {
            OrderCreatedAtDTO dto = new OrderCreatedAtDTO();
            dto.setOrderId(l.getOrderId());
            dto.setCreatedAt(l.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());

        return R.success(result);
    }
}
