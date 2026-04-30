package com.letsvpn.pay.mapper;

import com.letsvpn.common.core.dto.ChannelOpenRateRow;
import com.letsvpn.common.core.dto.ChannelSuccessRatePoint;
import com.letsvpn.common.core.dto.DashboardAmountMetric;
import com.letsvpn.common.core.dto.DashboardCountMetric;
import com.letsvpn.common.core.dto.DashboardSuccessRateMetric;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DashboardSummaryMapper {

    @Select("""
            SELECT
                today_amount AS todayAmount,
                yesterday_amount AS yesterdayAmount,
                CASE
                    WHEN yesterday_amount = 0 THEN NULL
                    ELSE ROUND((today_amount - yesterday_amount) / yesterday_amount * 100, 2)
                END AS changePercent
            FROM (
                SELECT
                    IFNULL((
                        SELECT SUM(real_amount)
                        FROM order_info
                        WHERE status = 1
                          AND pay_time >= CURDATE()
                          AND pay_time < CURDATE() + INTERVAL 1 DAY
                    ), 0) AS today_amount,
                    IFNULL((
                        SELECT SUM(real_amount)
                        FROM order_info
                        WHERE status = 1
                          AND pay_time >= CURDATE() - INTERVAL 1 DAY
                          AND pay_time < CURDATE()
                    ), 0) AS yesterday_amount
            ) t
            """)
    DashboardAmountMetric selectTransactionAmount();

    @Select("""
            SELECT
                today_count AS todayCount,
                yesterday_count AS yesterdayCount,
                CASE
                    WHEN yesterday_count = 0 THEN NULL
                    ELSE ROUND((today_count - yesterday_count) / yesterday_count * 100, 2)
                END AS changePercent
            FROM (
                SELECT
                    IFNULL((
                        SELECT COUNT(*)
                        FROM order_info
                        WHERE status = 1
                          AND pay_time >= CURDATE()
                          AND pay_time < CURDATE() + INTERVAL 1 DAY
                    ), 0) AS today_count,
                    IFNULL((
                        SELECT COUNT(*)
                        FROM order_info
                        WHERE status = 1
                          AND pay_time >= CURDATE() - INTERVAL 1 DAY
                          AND pay_time < CURDATE()
                    ), 0) AS yesterday_count
            ) t
            """)
    DashboardCountMetric selectTransactionCount();

    @Select("""
            SELECT
                CASE
                    WHEN curr_total = 0 THEN NULL
                    ELSE ROUND(curr_success / curr_total * 100, 2)
                END AS currentRate,
                CASE
                    WHEN prev_total = 0 THEN NULL
                    ELSE ROUND(prev_success / prev_total * 100, 2)
                END AS previousRate,
                CASE
                    WHEN prev_total = 0 OR prev_success = 0 THEN NULL
                    ELSE ROUND(
                        (
                            (curr_success / NULLIF(curr_total, 0))
                            - (prev_success / NULLIF(prev_total, 0))
                        )
                        / (prev_success / NULLIF(prev_total, 0)) * 100, 2
                    )
                END AS changePercent
            FROM (
                SELECT
                    SUM(
                        CASE
                            WHEN create_time >= NOW() - INTERVAL 10 MINUTE THEN
                                CASE WHEN status = 1 THEN 1 ELSE 0 END
                            ELSE 0
                        END
                    ) AS curr_success,
                    SUM(
                        CASE
                            WHEN create_time >= NOW() - INTERVAL 10 MINUTE THEN 1
                            ELSE 0
                        END
                    ) AS curr_total,
                    SUM(
                        CASE
                            WHEN create_time >= NOW() - INTERVAL 20 MINUTE
                             AND create_time < NOW() - INTERVAL 10 MINUTE THEN
                                CASE WHEN status = 1 THEN 1 ELSE 0 END
                            ELSE 0
                        END
                    ) AS prev_success,
                    SUM(
                        CASE
                            WHEN create_time >= NOW() - INTERVAL 20 MINUTE
                             AND create_time < NOW() - INTERVAL 10 MINUTE THEN 1
                            ELSE 0
                        END
                    ) AS prev_total
                FROM order_info
            ) t
            """)
    DashboardSuccessRateMetric selectSuccessRateWindow();

    @Select("""
            SELECT
                o.pay_config_id AS payConfigId,
                IFNULL(
                    CONCAT(
                        IFNULL(p.short_code, ''),
                        '(M', o.pay_config_id, ')-',
                        IFNULL(p.third_service, 'DP')
                    ),
                    CONCAT('Channel-', o.pay_config_id)
                ) AS channelName,
                DATE_FORMAT(
                    FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(o.create_time) / 300) * 300),
                    '%Y-%c-%e, %H:%i'
                ) AS windowTime,
                ROUND(SUM(CASE WHEN o.status = 1 THEN 1 ELSE 0 END) / COUNT(*) * 100, 2) AS successRate
            FROM order_info o
            LEFT JOIN pay_config_info p ON o.pay_config_id = p.id
            WHERE o.create_time >= #{startTime}
              AND o.create_time < #{endTime}
              AND o.pay_config_id IS NOT NULL
            GROUP BY o.pay_config_id, p.short_code, p.third_service, windowTime
            ORDER BY windowTime ASC, o.pay_config_id ASC
            """)
    List<ChannelSuccessRatePoint> selectChannelSuccessRate(
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime);

    @Select("""
            SELECT
                IFNULL(m.app_id, CONCAT('P', o.platform_id)) AS merchant,
                COUNT(*) AS orderNum,
                SUM(CASE WHEN o.create_status = 1 THEN 1 ELSE 0 END) AS openNum,
                ROUND(
                    CASE WHEN COUNT(*) = 0 THEN NULL
                    ELSE SUM(CASE WHEN o.create_status = 1 THEN 1 ELSE 0 END) / COUNT(*) * 100
                    END, 2
                ) AS openRate,
                ROUND(
                    CASE WHEN COUNT(*) = 0 THEN NULL
                    ELSE SUM(CASE WHEN o.status = 1 THEN 1 ELSE 0 END) / COUNT(*) * 100
                    END, 2
                ) AS successRate,
                ROUND(
                    CASE
                        WHEN SUM(CASE WHEN o.create_status = 1 THEN 1 ELSE 0 END) = 0 THEN NULL
                        ELSE SUM(CASE WHEN o.status = 1 THEN 1 ELSE 0 END) /
                             SUM(CASE WHEN o.create_status = 1 THEN 1 ELSE 0 END) * 100
                    END, 2
                ) AS openSuccessRate
            FROM order_info o
            LEFT JOIN (
                SELECT DISTINCT platform_id, app_id FROM merchant_info
            ) m ON o.platform_id = m.platform_id
            WHERE o.create_time >= NOW() - INTERVAL #{minutes} MINUTE
            GROUP BY o.platform_id, m.app_id
            ORDER BY orderNum DESC
            """)
    List<ChannelOpenRateRow> selectChannelOpenRate(@Param("minutes") int minutes);
}
