package com.letsvpn.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.admin.entity.Bill;
import com.letsvpn.admin.mapper.BillMapper;
import com.letsvpn.common.core.dto.BillOrderDetailDTO;
import com.letsvpn.common.core.dto.BillOrderIdsDTO;
import com.letsvpn.common.core.dto.BillPageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillService {

    private final BillMapper      billMapper;
    private final PayServiceFacade payServiceFacade;

    public Page<BillPageDTO> pageQuery(
            Long    id,
            String  account,
            String  channelTitle,
            Integer status,
            long    pageNum,
            long    pageSize) {

        long size  = Math.max(1L, Math.min(pageSize, 200L));
        long index = Math.max(pageNum, 1L);

        LambdaQueryWrapper<Bill> wrapper = Wrappers.<Bill>lambdaQuery()
                .orderByDesc(Bill::getCreatedAt);

        if (id != null)                          wrapper.eq(Bill::getId, id);
        if (StringUtils.hasText(account))        wrapper.like(Bill::getAccount, account.trim());
        if (StringUtils.hasText(channelTitle))   wrapper.like(Bill::getChannelTitle, channelTitle.trim());
        if (status != null)                      wrapper.eq(Bill::getStatus, status);

        Page<Bill> billPage = billMapper.selectPage(new Page<>(index, size), wrapper);

        List<Bill> records = billPage.getRecords();
        if (records == null || records.isEmpty()) {
            Page<BillPageDTO> empty = new Page<>(index, size, 0);
            empty.setRecords(Collections.emptyList());
            return empty;
        }

        // collect bill IDs and fetch order_ids via Feign
        List<Long> billIds = records.stream().map(Bill::getId).collect(Collectors.toList());
        Map<Long, String> orderIdsMap = fetchOrderIdsMap(billIds);

        List<BillPageDTO> dtos = records.stream().map(b -> {
            BillPageDTO dto = new BillPageDTO();
            dto.setId(b.getId());
            dto.setAccount(b.getAccount());
            dto.setChannelTitle(b.getChannelTitle());
            dto.setAmount(b.getAmount());
            dto.setStatus(b.getStatus());
            dto.setSettleAt(b.getSettleAt());
            dto.setCreatedAt(b.getCreatedAt());
            dto.setOrderIds(orderIdsMap.getOrDefault(b.getId(), ""));
            return dto;
        }).collect(Collectors.toList());

        Page<BillPageDTO> result = new Page<>(billPage.getCurrent(), billPage.getSize(), billPage.getTotal());
        result.setRecords(dtos);
        return result;
    }

    public List<BillOrderDetailDTO> getOrderDetails(Long billId) {
        return payServiceFacade.fetchOrderDetailsByBillId(billId);
    }

    private Map<Long, String> fetchOrderIdsMap(List<Long> billIds) {
        try {
            List<BillOrderIdsDTO> list = payServiceFacade.fetchOrderIdsByBillIds(billIds);
            if (list == null) return Collections.emptyMap();
            return list.stream()
                    .filter(d -> d.getBillId() != null)
                    .collect(Collectors.toMap(BillOrderIdsDTO::getBillId,
                            d -> d.getOrderIds() != null ? d.getOrderIds() : "",
                            (a, b) -> a));
        } catch (Exception e) {
            log.warn("Failed to fetch order ids for bills: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
