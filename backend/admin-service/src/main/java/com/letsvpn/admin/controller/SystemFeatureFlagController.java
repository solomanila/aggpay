package com.letsvpn.admin.controller;

import com.letsvpn.admin.dto.SystemFeatureFlagRequest;
import com.letsvpn.admin.entity.SystemFeatureFlag;
import com.letsvpn.admin.service.system.SystemFeatureFlagService;
import com.letsvpn.common.core.response.R;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/system/feature-flags")
@RequiredArgsConstructor
public class SystemFeatureFlagController {

    private final SystemFeatureFlagService featureFlagService;

    @GetMapping
    public R<List<SystemFeatureFlag>> list() {
        return R.success(featureFlagService.listCached());
    }

    @PostMapping
    public R<SystemFeatureFlag> create(@Valid @RequestBody SystemFeatureFlagRequest request) {
        SystemFeatureFlag entity = new SystemFeatureFlag();
        BeanUtils.copyProperties(request, entity);
        featureFlagService.save(entity);
        featureFlagService.evictCache();
        return R.success(entity);
    }

    @PutMapping("/{id}")
    public R<SystemFeatureFlag> update(@PathVariable Long id, @Valid @RequestBody SystemFeatureFlagRequest request) {
        SystemFeatureFlag entity = featureFlagService.getById(id);
        if (entity == null) {
            return R.fail("特性不存在");
        }
        BeanUtils.copyProperties(request, entity, "id");
        featureFlagService.updateById(entity);
        featureFlagService.evictCache();
        return R.success(entity);
    }
}
