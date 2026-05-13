package com.letsvpn.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.letsvpn.admin.entity.SystemUserAuth;
import com.letsvpn.admin.mapper.SystemUserAuthMapper;
import com.letsvpn.common.core.dto.PlatformAccountDTO;
import com.letsvpn.common.core.response.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/internal/user-auth")
@RequiredArgsConstructor
public class UserAccountInternalController {

    private final SystemUserAuthMapper systemUserAuthMapper;

    @GetMapping("/accounts-by-platform-ids")
    public R<List<PlatformAccountDTO>> getAccountsByPlatformIds(
            @RequestParam("platformIds") List<Integer> platformIds) {
        if (platformIds == null || platformIds.isEmpty()) {
            return R.success(Collections.emptyList());
        }
        List<SystemUserAuth> users = systemUserAuthMapper.selectList(
                new LambdaQueryWrapper<SystemUserAuth>()
                        .in(SystemUserAuth::getPlatformId, platformIds));
        List<PlatformAccountDTO> result = users.stream().map(u -> {
            PlatformAccountDTO dto = new PlatformAccountDTO();
            dto.setPlatformId(u.getPlatformId());
            dto.setAccount(u.getAccount());
            return dto;
        }).collect(Collectors.toList());
        return R.success(result);
    }

    @GetMapping("/platform-id-by-account")
    public R<Integer> getPlatformIdByAccount(@RequestParam("account") String account) {
        SystemUserAuth user = systemUserAuthMapper.selectOne(
                new LambdaQueryWrapper<SystemUserAuth>()
                        .eq(SystemUserAuth::getAccount, account)
                        .last("LIMIT 1"));
        return R.success(user != null ? user.getPlatformId() : null);
    }
}
