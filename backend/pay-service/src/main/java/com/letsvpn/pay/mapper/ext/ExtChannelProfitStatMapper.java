package com.letsvpn.pay.mapper.ext;

import com.letsvpn.common.core.dto.ChannelPlatformStatDTO;
import com.letsvpn.common.core.dto.ChannelProfitStatDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExtChannelProfitStatMapper {

    /**
     * 统计指定日期所有通道在 order_info 中 status=1 的 real_amount 之和。
     * 以 pay_config_channel 为驱动，LEFT JOIN 保证无成交的通道也出现在结果中（金额为 0）。
     */
    @Results({
        @Result(column = "channel_id",   property = "channelId"),
        @Result(column = "channel_name", property = "channelName"),
        @Result(column = "channel_type", property = "channelType"),
        @Result(column = "system_amount",property = "systemAmount"),
        @Result(column = "stat_date",    property = "statDate")
    })
    @Select("SELECT " +
            "    c.id              AS channel_id, " +
            "    c.title           AS channel_name, " +
            "    'PAYMENT'         AS channel_type, " +
            "    #{statDate}       AS stat_date, " +
            "    COALESCE(SUM(o.real_amount), 0) AS system_amount " +
            "FROM pay_config_channel c " +
            "LEFT JOIN order_info o " +
            "    ON o.pay_config_channel_id = c.id " +
            "   AND o.status = 1 " +
            "   AND DATE(o.create_time) = #{statDate} " +
            "GROUP BY c.id, c.title")
    List<ChannelProfitStatDTO> queryDailyStats(@Param("statDate") String statDate);

    /**
     * 统计指定日期所有通道在 order_info 中 status=1 的 real_amount，
     * 按 (channel_id, platform_id) 分组，用于计算各商户的通道费收入。
     */
    @Results({
        @Result(column = "channel_id",   property = "channelId"),
        @Result(column = "channel_name", property = "channelName"),
        @Result(column = "platform_id",  property = "platformId"),
        @Result(column = "system_amount",property = "systemAmount"),
        @Result(column = "stat_date",    property = "statDate")
    })
    @Select("SELECT " +
            "    c.id              AS channel_id, " +
            "    c.title           AS channel_name, " +
            "    o.platform_id     AS platform_id, " +
            "    #{statDate}       AS stat_date, " +
            "    COALESCE(SUM(o.real_amount), 0) AS system_amount " +
            "FROM pay_config_channel c " +
            "INNER JOIN order_info o " +
            "    ON o.pay_config_channel_id = c.id " +
            "   AND o.status = 1 " +
            "   AND DATE(o.create_time) = #{statDate} " +
            "GROUP BY c.id, c.title, o.platform_id")
    List<ChannelPlatformStatDTO> queryDailyStatsByPlatform(@Param("statDate") String statDate);
}
