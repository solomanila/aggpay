package com.letsvpn.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.admin.dto.MerchantCreateResponse;
import com.letsvpn.admin.dto.MerchantListItemDTO;
import com.letsvpn.admin.dto.MerchantPageRequest;
import com.letsvpn.admin.dto.MerchantSaveRequest;
import com.letsvpn.admin.entity.MerchantBalance;
import com.letsvpn.admin.entity.MerchantChannelConfig;
import com.letsvpn.admin.entity.MerchantOpConfig;
import com.letsvpn.admin.entity.MerchantProfile;
import com.letsvpn.admin.entity.SystemRole;
import com.letsvpn.admin.entity.SystemUserAuth;
import com.letsvpn.admin.entity.SystemUserRole;
import com.letsvpn.admin.mapper.MerchantBalanceMapper;
import com.letsvpn.admin.mapper.MerchantChannelConfigMapper;
import com.letsvpn.admin.mapper.MerchantOpConfigMapper;
import com.letsvpn.admin.mapper.MerchantProfileMapper;
import com.letsvpn.admin.mapper.SystemRoleMapper;
import com.letsvpn.admin.mapper.SystemUserAuthMapper;
import com.letsvpn.admin.mapper.SystemUserRoleMapper;
import com.letsvpn.admin.util.GoogleAuthenticatorUtil;
import com.letsvpn.common.core.dto.MerchantProfileDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantManageService {

    private final MerchantOpConfigMapper opConfigMapper;
    private final MerchantBalanceMapper balanceMapper;
    private final MerchantChannelConfigMapper channelConfigMapper;
    private final MerchantProfileMapper profileMapper;
    private final PayServiceFacade payServiceFacade;
    private final SystemUserAuthMapper userAuthMapper;
    private final SystemUserRoleMapper userRoleMapper;
    private final SystemRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    /** 分页查询商户列表 */
    public Page<MerchantListItemDTO> page(MerchantPageRequest req) {
        Page<MerchantProfileDTO> profilePage = payServiceFacade.fetchMerchantPage(
                req.getKeyword(), req.getStatus(), req.getPageNum(), req.getPageSize());

        List<Integer> ids = profilePage.getRecords().stream()
                .map(MerchantProfileDTO::getPlatformId).collect(Collectors.toList());

        Map<Integer, MerchantOpConfig> opMap = Collections.emptyMap();
        if (!ids.isEmpty()) {
            LambdaQueryWrapper<MerchantOpConfig> opQw = new LambdaQueryWrapper<MerchantOpConfig>()
                    .in(MerchantOpConfig::getPlatformId, ids)
                    .eq(req.getAgentId() != null, MerchantOpConfig::getAgentId, req.getAgentId());
            opMap = opConfigMapper.selectList(opQw).stream()
                    .collect(Collectors.toMap(MerchantOpConfig::getPlatformId, o -> o, (a, b) -> a));
        }

        Map<Integer, List<MerchantBalance>> balMap = Collections.emptyMap();
        if (!ids.isEmpty()) {
            balMap = balanceMapper.selectList(
                    new LambdaQueryWrapper<MerchantBalance>().in(MerchantBalance::getPlatformId, ids)
            ).stream().collect(Collectors.groupingBy(MerchantBalance::getPlatformId));
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<Integer, MerchantOpConfig> finalOpMap = opMap;
        Map<Integer, List<MerchantBalance>> finalBalMap = balMap;

        List<MerchantListItemDTO> dtos = profilePage.getRecords().stream()
                .filter(p -> req.getAgentId() == null || finalOpMap.containsKey(p.getPlatformId()))
                .map(p -> {
                    MerchantListItemDTO dto = new MerchantListItemDTO();
                    dto.setPlatformId(p.getPlatformId());
                    dto.setTitle(p.getTitle());
                    dto.setKeyId(p.getPlatformNo());
                    dto.setAppKey(p.getSecretKey());
                    dto.setStatus(p.getNullify() != null && p.getNullify() == 0 ? 1 : 0);
                    dto.setCreatedAt(p.getCreateTime() != null ? sdf.format(p.getCreateTime()) : "");

                    MerchantOpConfig op = finalOpMap.get(p.getPlatformId());
                    if (op != null) {
                        dto.setAgentId(op.getAgentId());
                        dto.setEmail(op.getEmail());
                        dto.setRemark(op.getRemark());
                    }

                    List<MerchantBalance> bals = finalBalMap.getOrDefault(p.getPlatformId(), Collections.emptyList());
                    if (!bals.isEmpty()) {
                        dto.setBalanceSummary(bals.stream()
                                .map(b -> b.getCurrency() + ":" + b.getAvailable().stripTrailingZeros().toPlainString())
                                .collect(Collectors.joining(",")));
                    }
                    return dto;
                }).collect(Collectors.toList());

        Page<MerchantListItemDTO> result = new Page<>(
                profilePage.getCurrent(), profilePage.getSize(), profilePage.getTotal());
        result.setRecords(dtos);
        return result;
    }

    /** 查询单商户详情（用于编辑回填） */
    public Map<String, Object> detail(Integer platformId) {
        MerchantProfileDTO info = payServiceFacade.fetchMerchantDetail(platformId);

        MerchantOpConfig op = opConfigMapper.selectOne(
                new LambdaQueryWrapper<MerchantOpConfig>().eq(MerchantOpConfig::getPlatformId, platformId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("platformId", info.getPlatformId());
        result.put("title", info.getTitle());
        result.put("keyId", info.getPlatformNo());
        result.put("appKey", info.getSecretKey());
        result.put("status", info.getNullify() != null && info.getNullify() == 0 ? 1 : 0);
        if (op != null) {
            result.put("agentId", op.getAgentId());
            result.put("email", op.getEmail());
            result.put("remark", op.getRemark());
            result.put("dailyPayOrderLimit", op.getDailyPayOrderLimit());
            result.put("dailyWithdrawCountLimit", op.getDailyWithdrawCountLimit());
            result.put("dailyWithdrawAmountLimit", op.getDailyWithdrawAmountLimit());
            result.put("dailyPayLimit", op.getDailyPayLimit());
            result.put("dailyPayoutLimit", op.getDailyPayoutLimit());
            result.put("largePayoutRiskEnabled", op.getLargePayoutRiskEnabled());
            result.put("largePayoutRiskAmount", op.getLargePayoutRiskAmount());
            result.put("telegramGroupId", op.getTelegramGroupId());
            result.put("settlementNotify", op.getSettlementNotify());
        }
        return result;
    }

    /** 创建商户：同时初始化 balance、channel_config（status=0 通道）、merchant_profile、system_user_auth */
    @Transactional
    public MerchantCreateResponse create(MerchantSaveRequest req) {
        // 1. 在 pay-service 创建 pay_platform_info
        MerchantProfileDTO created = payServiceFacade.createMerchant(req.getTitle(), req.getStatus());
        Integer platformId = created.getPlatformId();

        // 2. admin.merchant_op_config
        upsertOpConfig(platformId, req);

        // 3. admin.merchant_balance (INR, available=0)
        MerchantBalance balance = new MerchantBalance();
        balance.setPlatformId(platformId);
        balance.setCurrency("INR");
        balance.setAvailable(BigDecimal.ZERO);
        balance.setFrozen(BigDecimal.ZERO);
        balanceMapper.insert(balance);

        // 4. admin.merchant_channel_config：为每个 status=0 的通道创建一条记录
        List<Long> activeChannelIds = payServiceFacade.fetchActiveChannelIds();
        if (!activeChannelIds.isEmpty()) {
            for (Long channelId : activeChannelIds) {
                MerchantChannelConfig cfg = new MerchantChannelConfig();
                cfg.setPlatformId(platformId);
                cfg.setPayConfigChannelId(channelId);
                cfg.setChannelType("PAYMENT");
                cfg.setDailyLimit(new BigDecimal("5000000"));
                cfg.setWeight(1);
                cfg.setEnabled(1);
                cfg.setCurrency("INR");
                cfg.setFeeRate(BigDecimal.ZERO);
                cfg.setFeeFixed(BigDecimal.ZERO);
                cfg.setTieredRateEnabled(0);
                cfg.setAutoSettle(0);
                cfg.setMinAmount(BigDecimal.ZERO);
                channelConfigMapper.insert(cfg);
            }
        }

        // 5. admin.merchant_profile
        MerchantProfile profile = new MerchantProfile();
        profile.setPlatformId(platformId);
        profile.setMerchantCode("MER-" + platformId);
        profile.setName(req.getTitle());
        profile.setStatus("ACTIVE");
        profileMapper.insert(profile);

        // 6. admin.system_user_auth — 创建商户登录账号
        long exists = userAuthMapper.selectCount(
                new LambdaQueryWrapper<SystemUserAuth>()
                        .eq(SystemUserAuth::getAccount, req.getAccount()));
        if (exists > 0) {
            throw new IllegalArgumentException("账号已存在: " + req.getAccount());
        }
        String secret = GoogleAuthenticatorUtil.generateSecret();
        LocalDateTime now = LocalDateTime.now();
        SystemUserAuth user = new SystemUserAuth();
        user.setAccount(req.getAccount());
        user.setName(req.getTitle());
        user.setEmail(req.getEmail());
        user.setStatus("ACTIVE");
        user.setRiskLevel("LOW");
        user.setPasswordAlgo("bcrypt");
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setGoogleSecret(secret);
        user.setGoogleEnabled(1);
        user.setTags("[\"MERCHANT\"]");
        user.setPlatformId(platformId);
        user.setForceReset(0);
        user.setPasswordUpdatedAt(now);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userAuthMapper.insert(user);

        // 7. system_user_role — 绑定 merchant 角色
        SystemRole merchantRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SystemRole>()
                        .eq(SystemRole::getRoleCode, "merchant"));
        if (merchantRole != null) {
            SystemUserRole ur = new SystemUserRole();
            ur.setUserId(user.getId());
            ur.setRoleId(merchantRole.getId());
            userRoleMapper.insert(ur);
        }

        MerchantCreateResponse resp = new MerchantCreateResponse();
        resp.setPlatformId(platformId);
        resp.setUserId(user.getId());
        resp.setAccount(user.getAccount());
        resp.setGoogleSecret(secret);
        resp.setOtpAuthUrl(GoogleAuthenticatorUtil.buildOtpAuthUrl(
                user.getAccount(), "PayAdmin", secret));
        return resp;
    }

    /** 更新商户 */
    @Transactional
    public void update(MerchantSaveRequest req) {
        if (req.getPlatformId() == null) throw new IllegalArgumentException("platformId 不能为空");
        String title = StringUtils.hasText(req.getTitle()) ? req.getTitle() : null;
        payServiceFacade.updateMerchant(req.getPlatformId(), title, req.getStatus());
        upsertOpConfig(req.getPlatformId(), req);
    }

    /** 切换状态：前端传 status=1(启用)/0(禁用) */
    public void toggleStatus(Integer platformId, Integer status) {
        payServiceFacade.toggleMerchantStatus(platformId, status);
    }

    /** 删除商户：同时清理 balance、channel_config、merchant_profile、op_config */
    @Transactional
    public void delete(Integer platformId) {
        payServiceFacade.deleteMerchant(platformId);
        opConfigMapper.delete(new LambdaQueryWrapper<MerchantOpConfig>()
                .eq(MerchantOpConfig::getPlatformId, platformId));
        balanceMapper.delete(new LambdaQueryWrapper<MerchantBalance>()
                .eq(MerchantBalance::getPlatformId, platformId));
        channelConfigMapper.delete(new LambdaQueryWrapper<MerchantChannelConfig>()
                .eq(MerchantChannelConfig::getPlatformId, platformId));
        profileMapper.delete(new LambdaQueryWrapper<MerchantProfile>()
                .eq(MerchantProfile::getPlatformId, platformId));
    }

    /** 重置 secretKey */
    public String resetAppKey(Integer platformId) {
        return payServiceFacade.resetMerchantKey(platformId);
    }

    // ── private ────────────────────────────────────────────────────

    private void upsertOpConfig(Integer platformId, MerchantSaveRequest req) {
        MerchantOpConfig existing = opConfigMapper.selectOne(
                new LambdaQueryWrapper<MerchantOpConfig>().eq(MerchantOpConfig::getPlatformId, platformId));
        MerchantOpConfig op = existing != null ? existing : new MerchantOpConfig();
        op.setPlatformId(platformId);
        if (req.getAgentId() != null)                   op.setAgentId(req.getAgentId());
        if (req.getEmail() != null)                     op.setEmail(req.getEmail());
        if (req.getRemark() != null)                    op.setRemark(req.getRemark());
        if (req.getDailyPayOrderLimit() != null)        op.setDailyPayOrderLimit(req.getDailyPayOrderLimit());
        if (req.getDailyWithdrawCountLimit() != null)   op.setDailyWithdrawCountLimit(req.getDailyWithdrawCountLimit());
        if (req.getDailyWithdrawAmountLimit() != null)  op.setDailyWithdrawAmountLimit(req.getDailyWithdrawAmountLimit());
        if (req.getDailyPayLimit() != null)             op.setDailyPayLimit(req.getDailyPayLimit());
        if (req.getDailyPayoutLimit() != null)          op.setDailyPayoutLimit(req.getDailyPayoutLimit());
        if (req.getLargePayoutRiskEnabled() != null)    op.setLargePayoutRiskEnabled(req.getLargePayoutRiskEnabled());
        if (req.getLargePayoutRiskAmount() != null)     op.setLargePayoutRiskAmount(req.getLargePayoutRiskAmount());
        if (req.getTelegramGroupId() != null)           op.setTelegramGroupId(req.getTelegramGroupId());
        if (req.getSettlementNotify() != null)          op.setSettlementNotify(req.getSettlementNotify());

        if (existing == null) opConfigMapper.insert(op);
        else opConfigMapper.updateById(op);
    }
}
