package com.letsvpn.admin.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.letsvpn.admin.dto.SystemUserUpdateRequest;
import com.letsvpn.admin.dto.SystemUserVO;
import com.letsvpn.admin.entity.SystemUserAuth;
import com.letsvpn.admin.service.system.SystemUserAuthService;
import com.letsvpn.common.core.response.R;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/system/users")
@RequiredArgsConstructor
@Slf4j
public class SystemUserController {

    private final SystemUserAuthService systemUserAuthService;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public R<List<SystemUserVO>> listUsers() {
        List<SystemUserAuth> users = systemUserAuthService.listCached();
        List<SystemUserVO> result = users.stream().map(this::convert).collect(Collectors.toList());
        return R.success(result);
    }

    @GetMapping("/{id}")
    public R<SystemUserVO> getUser(@PathVariable Long id) {
        SystemUserAuth user = systemUserAuthService.getById(id);
        if (user == null) {
            return R.fail("用户不存在");
        }
        return R.success(convert(user));
    }

    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody @Valid SystemUserUpdateRequest request) {
        SystemUserAuth user = systemUserAuthService.getById(id);
        if (user == null) {
            return R.fail("用户不存在");
        }
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getMobile() != null) {
            user.setMobile(request.getMobile());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (request.getRiskLevel() != null) {
            user.setRiskLevel(request.getRiskLevel());
        }
        if (request.getOwnerUserId() != null) {
            user.setOwnerUserId(request.getOwnerUserId());
        }
        if (request.getTags() != null) {
            try {
                user.setTags(objectMapper.writeValueAsString(request.getTags()));
            } catch (Exception e) {
                return R.fail("标签格式错误");
            }
        }
        if (StringUtils.hasText(request.getNewPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            user.setPasswordUpdatedAt(LocalDateTime.now());
        }
        user.setUpdatedAt(LocalDateTime.now());
        boolean updated = systemUserAuthService.updateById(user);
        systemUserAuthService.evictUserCache();
        return updated ? R.success(Boolean.TRUE) : R.fail("更新失败");
    }

    private SystemUserVO convert(SystemUserAuth user) {
        SystemUserVO vo = new SystemUserVO();
        vo.setId(user.getId());
        vo.setAccount(user.getAccount());
        vo.setName(user.getName());
        vo.setEmail(user.getEmail());
        vo.setMobile(user.getMobile());
        vo.setStatus(user.getStatus());
        vo.setRiskLevel(user.getRiskLevel());
        vo.setOwnerUserId(user.getOwnerUserId());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setTags(parseTags(user.getTags()));
        return vo;
    }

    private List<String> parseTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(tags, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse user tags {}", tags, e);
            return Collections.emptyList();
        }
    }
}
