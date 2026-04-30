package com.letsvpn.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.admin.dto.ChannelLimitPlanRequest;
import com.letsvpn.admin.entity.PayChannelLimitPlan;
import com.letsvpn.admin.mapper.PayChannelLimitPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChannelLimitPlanService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    private final PayChannelLimitPlanMapper mapper;

    public Page<PayChannelLimitPlan> list(String channelId, long pageNum, long pageSize) {
        long idx = Math.max(pageNum, 1L);
        long size = Math.max(1L, Math.min(pageSize, 200L));

        LambdaQueryWrapper<PayChannelLimitPlan> wrapper = Wrappers.<PayChannelLimitPlan>lambdaQuery()
                .orderByDesc(PayChannelLimitPlan::getId);
        if (channelId != null && !channelId.isBlank()) {
            wrapper.like(PayChannelLimitPlan::getChannelId, channelId.trim());
        }
        return mapper.selectPage(new Page<>(idx, size), wrapper);
    }

    public void updateStatus(Long id, String status) {
        if (id == null) throw new IllegalArgumentException("id is required");
        String normalized = STATUS_ACTIVE.equalsIgnoreCase(status) ? STATUS_ACTIVE : STATUS_INACTIVE;
        mapper.update(null, new LambdaUpdateWrapper<PayChannelLimitPlan>()
                .set(PayChannelLimitPlan::getStatus, normalized)
                .eq(PayChannelLimitPlan::getId, id));
    }

    public void create(ChannelLimitPlanRequest req) {
        PayChannelLimitPlan plan = new PayChannelLimitPlan();
        plan.setChannelId(req.getChannelId());
        plan.setPlanType(req.getPlanType());
        plan.setWindowMinutes(req.getWindowMinutes());
        plan.setStatus(req.getStatus() != null ? req.getStatus() : STATUS_INACTIVE);
        mapper.insert(plan);
    }

    public void update(ChannelLimitPlanRequest req) {
        if (req.getId() == null) throw new IllegalArgumentException("id is required");
        PayChannelLimitPlan plan = new PayChannelLimitPlan();
        plan.setId(req.getId());
        if (req.getChannelId() != null) plan.setChannelId(req.getChannelId());
        if (req.getPlanType() != null) plan.setPlanType(req.getPlanType());
        if (req.getWindowMinutes() != null) plan.setWindowMinutes(req.getWindowMinutes());
        if (req.getStatus() != null) plan.setStatus(req.getStatus());
        mapper.updateById(plan);
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }
}
