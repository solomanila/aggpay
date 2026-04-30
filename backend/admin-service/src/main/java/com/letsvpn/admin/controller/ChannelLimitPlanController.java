package com.letsvpn.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.admin.dto.ChannelLimitPlanRequest;
import com.letsvpn.admin.entity.PayChannelLimitPlan;
import com.letsvpn.admin.service.ChannelLimitPlanService;
import com.letsvpn.common.core.response.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/channel-limit")
@RequiredArgsConstructor
public class ChannelLimitPlanController {

    private final ChannelLimitPlanService service;

    @GetMapping("/list")
    public R<Page<PayChannelLimitPlan>> list(
            @RequestParam(value = "channelId", required = false) String channelId,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        return R.success(service.list(channelId, pageNum, pageSize));
    }

    @PatchMapping("/status")
    public R<Void> updateStatus(
            @RequestParam("id") Long id,
            @RequestParam("status") String status) {
        service.updateStatus(id, status);
        return R.success(null);
    }

    @PostMapping("/create")
    public R<Void> create(@RequestBody ChannelLimitPlanRequest req) {
        service.create(req);
        return R.success(null);
    }

    @PutMapping("/update")
    public R<Void> update(@RequestBody ChannelLimitPlanRequest req) {
        service.update(req);
        return R.success(null);
    }

    @DeleteMapping("/delete")
    public R<Void> delete(@RequestParam("id") Long id) {
        service.delete(id);
        return R.success(null);
    }
}
