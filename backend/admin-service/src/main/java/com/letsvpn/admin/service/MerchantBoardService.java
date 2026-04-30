package com.letsvpn.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.letsvpn.admin.entity.MerchantChannelConfig;
import com.letsvpn.admin.mapper.MerchantChannelConfigMapper;
import com.letsvpn.common.core.dto.BoardChannelDTO;
import com.letsvpn.common.core.dto.MerchantProfileDTO;
import com.letsvpn.common.core.dto.PayConfigChannelDTO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantBoardService {

    private final MerchantChannelConfigMapper channelConfigMapper;
    private final PayServiceFacade payServiceFacade;

    // ── Board data ────────────────────────────────────────────────

    public BoardData boardData(String channelType, Integer merchantId, Long channelId, String currency) {
        // 1. All configs for this channelType
        LambdaQueryWrapper<MerchantChannelConfig> cqw = new LambdaQueryWrapper<MerchantChannelConfig>()
                .eq(MerchantChannelConfig::getChannelType, channelType)
                .eq(merchantId != null, MerchantChannelConfig::getPlatformId, merchantId)
                .eq(channelId != null, MerchantChannelConfig::getPayConfigChannelId, channelId)
                .eq(StringUtils.hasText(currency), MerchantChannelConfig::getCurrency, currency);
        List<MerchantChannelConfig> configs = channelConfigMapper.selectList(cqw);

        // 2. Distinct channelIds used in configs → column headers via Feign
        Set<Long> usedChannelIds = configs.stream()
                .map(MerchantChannelConfig::getPayConfigChannelId).collect(Collectors.toSet());

        List<ChannelDTO> channels;
        if (!usedChannelIds.isEmpty()) {
            List<BoardChannelDTO> fetched = payServiceFacade.fetchChannelsByIds(new ArrayList<>(usedChannelIds));
            if (!fetched.isEmpty()) {
                channels = fetched.stream().map(c -> {
                    ChannelDTO dto = new ChannelDTO();
                    dto.setId(c.getId());
                    dto.setTitle(c.getTitle());
                    dto.setPayConfigId(c.getPayConfigId());
                    dto.setPayConfigTitle(c.getPayConfigTitle());
                    return dto;
                }).sorted(Comparator.comparing(ChannelDTO::getId)).collect(Collectors.toList());
            } else {
                // Feign returned empty: derive placeholder titles from known IDs
                channels = usedChannelIds.stream().sorted().map(cid -> {
                    ChannelDTO dto = new ChannelDTO();
                    dto.setId(cid);
                    dto.setTitle("CH-" + cid);
                    dto.setPayConfigId(0);
                    dto.setPayConfigTitle("");
                    return dto;
                }).collect(Collectors.toList());
            }
        } else {
            channels = Collections.emptyList();
        }

        // 3. Merchants: fetch all from Feign, supplement with any config platformIds missing from profiles
        List<MerchantProfileDTO> profiles = payServiceFacade.fetchMerchantProfiles();
        Map<Integer, MerchantDTO> merchantMap = new LinkedHashMap<>();

        // Add profiles from pay-service
        profiles.stream()
                .filter(p -> merchantId == null || merchantId.equals(p.getPlatformId()))
                .forEach(p -> {
                    MerchantDTO dto = new MerchantDTO();
                    dto.setPlatformId(p.getPlatformId());
                    dto.setTitle(p.getTitle());
                    merchantMap.put(p.getPlatformId(), dto);
                });

        // Supplement: any config platformId not covered by profiles gets a placeholder
        configs.stream()
                .map(MerchantChannelConfig::getPlatformId)
                .filter(pid -> !merchantMap.containsKey(pid))
                .filter(pid -> merchantId == null || merchantId.equals(pid))
                .forEach(pid -> {
                    MerchantDTO dto = new MerchantDTO();
                    dto.setPlatformId(pid);
                    dto.setTitle("M-" + pid);
                    merchantMap.put(pid, dto);
                });

        List<MerchantDTO> merchants = merchantMap.values().stream()
                .sorted(Comparator.comparing(MerchantDTO::getPlatformId))
                .collect(Collectors.toList());

        BoardData result = new BoardData();
        result.setMerchants(merchants);
        result.setChannels(channels);
        result.setConfigs(configs);
        return result;
    }

    // ── Channel options for modal select ─────────────────────────

    public List<ChannelDTO> channelOptions() {
        // Fetch all channels from pay-service (large page to get all)
        List<PayConfigChannelDTO> all = payServiceFacade.fetchChannelConfigList(null, null, 1, 1000).getRecords();
        if (all.isEmpty()) {
            // Fallback: derive from existing configs
            return channelConfigMapper.selectList(null).stream()
                    .collect(Collectors.toMap(MerchantChannelConfig::getPayConfigChannelId, c -> c, (a, b) -> a))
                    .values().stream()
                    .map(c -> {
                        ChannelDTO dto = new ChannelDTO();
                        dto.setId(c.getPayConfigChannelId());
                        dto.setTitle("CH-" + c.getPayConfigChannelId());
                        dto.setPayConfigId(0);
                        dto.setPayConfigTitle("");
                        return dto;
                    })
                    .sorted(Comparator.comparing(ChannelDTO::getId))
                    .collect(Collectors.toList());
        }
        return all.stream().map(c -> {
            ChannelDTO dto = new ChannelDTO();
            dto.setId(c.getId() != null ? c.getId().longValue() : 0L);
            dto.setTitle(c.getTitle());
            dto.setPayConfigId(c.getId());
            dto.setPayConfigTitle(c.getThirdService() != null ? c.getThirdService() : "");
            return dto;
        }).sorted(Comparator.comparing(ChannelDTO::getId)).collect(Collectors.toList());
    }

    // ── DTOs ──────────────────────────────────────────────────────

    @Data
    public static class BoardData {
        private List<MerchantDTO> merchants;
        private List<ChannelDTO> channels;
        private List<MerchantChannelConfig> configs;
    }

    @Data
    public static class MerchantDTO {
        private Integer platformId;
        private String title;
    }

    @Data
    public static class ChannelDTO {
        private Long id;
        private String title;
        private Integer payConfigId;
        private String payConfigTitle;
    }
}
