package com.letsvpn.admin.scheduler;

import com.letsvpn.admin.service.ChannelProfitStatService;
import com.letsvpn.admin.service.PayServiceFacade;
import com.letsvpn.common.core.dto.ChannelPlatformStatDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChannelProfitStatScheduler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PayServiceFacade payServiceFacade;
    private final ChannelProfitStatService channelProfitStatService;

    /** 每天凌晨 01:00 统计前一天各通道成交额和通道费收入 */
    @Scheduled(cron = "0 0 1 * * ?")
    public void runDailyChannelStat() {
        String yesterday = LocalDate.now().minusDays(1).format(DATE_FMT);
        log.info("ChannelProfitStatScheduler start, statDate={}", yesterday);
        try {
            List<ChannelPlatformStatDTO> stats = payServiceFacade.fetchDailyChannelPlatformStats(yesterday);
            channelProfitStatService.upsertDailyStat(yesterday, stats);
            log.info("ChannelProfitStatScheduler done, statDate={}", yesterday);
        } catch (Exception e) {
            log.error("ChannelProfitStatScheduler failed, statDate={}", yesterday, e);
        }
    }
}
