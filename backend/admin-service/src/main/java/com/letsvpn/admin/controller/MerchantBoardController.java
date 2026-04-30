package com.letsvpn.admin.controller;

import com.letsvpn.admin.service.MerchantBoardService;
import com.letsvpn.common.core.response.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商户-面板：商户 × 通道 路由矩阵
 */
@RestController
@RequestMapping("/admin/merchant-board")
@RequiredArgsConstructor
public class MerchantBoardController {

    private final MerchantBoardService service;

    /**
     * 获取面板矩阵数据
     * @param channelType PAYMENT | PAYOUT
     * @param merchantId  可选过滤
     * @param channelId   可选过滤
     * @param currency    可选过滤（INR / RUB…）
     */
    @GetMapping("/data")
    public R<MerchantBoardService.BoardData> data(
            @RequestParam(defaultValue = "PAYMENT") String channelType,
            @RequestParam(required = false) Integer merchantId,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) String currency) {
        return R.success(service.boardData(channelType, merchantId, channelId, currency));
    }

    /** 通道选项（供编辑弹窗下拉选择） */
    @GetMapping("/channel-options")
    public R<List<MerchantBoardService.ChannelDTO>> channelOptions() {
        return R.success(service.channelOptions());
    }
}
