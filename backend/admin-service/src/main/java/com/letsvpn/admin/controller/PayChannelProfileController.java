package com.letsvpn.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.letsvpn.admin.dto.ChannelProfilePageRequest;
import com.letsvpn.admin.dto.ChannelProfileVO;
import com.letsvpn.admin.entity.PayChannelProfile;
import com.letsvpn.admin.service.PayChannelProfileService;
import com.letsvpn.common.core.response.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/channel-profile")
@RequiredArgsConstructor
public class PayChannelProfileController {

    private final PayChannelProfileService service;
    private final ObjectMapper objectMapper;

    @GetMapping("/page")
    public R<IPage<ChannelProfileVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String areaType,
            @RequestParam(required = false) String businessType) {
        ChannelProfilePageRequest req = new ChannelProfilePageRequest();
        req.setPageNum(pageNum);
        req.setPageSize(pageSize);
        req.setAreaType(areaType);
        req.setBusinessType(businessType);
        return R.success(service.pageList(req));
    }

    @GetMapping("/options")
    public R<Map<String, List<String>>> options() {
        return R.success(service.options());
    }

    @GetMapping("/{id}")
    public R<PayChannelProfile> getById(@PathVariable Long id) {
        PayChannelProfile profile = service.getById(id);
        if (profile == null) {
            return R.fail("通道档案不存在: " + id);
        }
        return R.success(profile);
    }

    @PutMapping("/{id}/limit-config")
    public R<Void> upsertLimitConfig(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            service.upsertLimitConfig(id, objectMapper.writeValueAsString(body));
            return R.success(null);
        } catch (Exception e) {
            return R.fail("更新限流配置失败: " + e.getMessage());
        }
    }
}
