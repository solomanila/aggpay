package com.letsvpn.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.admin.entity.SystemUserAuth;
import com.letsvpn.admin.mapper.SystemUserAuthMapper;
import com.letsvpn.admin.service.PayServiceFacade;
import com.letsvpn.common.core.dto.OrderInfoDTO;
import com.letsvpn.common.core.response.R;
import com.letsvpn.common.core.util.AuthContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/merchant/orders")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final SystemUserAuthMapper userAuthMapper;
    private final PayServiceFacade payServiceFacade;

    @GetMapping("/payin")
    public R<Page<OrderInfoDTO>> payinOrders(
            @RequestParam(value = "orderId", required = false) String orderId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {

        Long userId = AuthContextHolder.getRequiredUserId();
        SystemUserAuth user = userAuthMapper.selectById(userId);
        if (user == null || user.getPlatformId() == null) {
            return R.fail("未绑定商户平台");
        }

        Page<OrderInfoDTO> page = payServiceFacade.fetchMerchantPayinOrders(
                user.getPlatformId(), orderId, startDate, endDate, status, pageNum, pageSize);
        return R.success(page);
    }
}
