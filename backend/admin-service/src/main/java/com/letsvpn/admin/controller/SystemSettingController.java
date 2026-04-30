package com.letsvpn.admin.controller;

import com.letsvpn.admin.dto.SystemSettingUpsertRequest;
import com.letsvpn.admin.entity.SystemSetting;
import com.letsvpn.admin.service.system.SystemSettingService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/system/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    public R<List<SystemSetting>> list(@RequestParam(value = "category", required = false) String category) {
        List<SystemSetting> settings = systemSettingService.listCached();
        if (category == null) {
            return R.success(settings);
        }
        return R.success(settings.stream()
                .filter(item -> category.equalsIgnoreCase(item.getCategory()))
                .toList());
    }

    @PostMapping
    public R<SystemSetting> create(@Valid @RequestBody SystemSettingUpsertRequest request) {
        SystemSetting setting = new SystemSetting();
        BeanUtils.copyProperties(request, setting);
        SystemSetting saved = systemSettingService.saveOrUpdateSetting(setting, request.getOwnerUserId(), "CREATE");
        return R.success(saved);
    }

    @PutMapping("/{id}")
    public R<SystemSetting> update(@PathVariable Long id, @Valid @RequestBody SystemSettingUpsertRequest request) {
        SystemSetting existing = systemSettingService.getById(id);
        if (existing == null) {
            return R.fail("配置不存在");
        }
        existing.setSettingName(request.getSettingName());
        existing.setValue(request.getValue());
        existing.setValueType(request.getValueType());
        existing.setCategory(request.getCategory());
        existing.setDescription(request.getDescription());
        existing.setStatus(request.getStatus());
        existing.setGrayScope(request.getGrayScope());
        existing.setOwnerUserId(request.getOwnerUserId());
        SystemSetting saved = systemSettingService.saveOrUpdateSetting(existing, request.getOwnerUserId(), "UPDATE");
        return R.success(saved);
    }
}
