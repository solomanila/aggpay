package com.letsvpn.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.admin.entity.ChannelProfitStat;
import com.letsvpn.admin.service.ChannelProfitStatService;
import com.letsvpn.common.core.response.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/channel-profit")
@RequiredArgsConstructor
public class ChannelProfitStatController {

    private final ChannelProfitStatService service;

    @PostMapping("/trigger")
    public R<String> trigger(@RequestParam("date") String date) {
        service.triggerForDate(date);
        return R.success("OK, date=" + date);
    }

    @GetMapping("/list")
    public R<Page<ChannelProfitStat>> list(
            @RequestParam(value = "channelName", required = false) String channelName,
            @RequestParam(value = "startDate",   required = false) String startDate,
            @RequestParam(value = "endDate",     required = false) String endDate,
            @RequestParam(value = "pageNum",     defaultValue = "1")  long pageNum,
            @RequestParam(value = "pageSize",    defaultValue = "20") long pageSize) {
        return R.success(service.list(channelName, startDate, endDate, pageNum, pageSize));
    }
}
