package com.letsvpn.pay.mapper.ext;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.letsvpn.common.core.dto.PayinSummaryRowDTO;
import com.letsvpn.pay.dto.MerchantRateDTO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExtPayinSummaryMapper {

    @Select("""
            <script>
            SELECT
                DATE_FORMAT(MIN(DATE(a.create_time)), '%Y-%c-%e') AS localTimeDay,
                a.platform_id AS platformId,
                ANY_VALUE(b.title) AS channel,
                COALESCE(SUM(CASE WHEN a.status = 1 THEN a.real_amount ELSE 0 END), 0) AS successAmount,
                SUM(CASE WHEN a.status = 1 THEN 1 ELSE 0 END) AS successCount,
                COUNT(*) AS orderNum,
                ROUND(
                    CASE WHEN COUNT(*) = 0 THEN 0
                    ELSE SUM(CASE WHEN a.status = 1 THEN 1 ELSE 0 END) / COUNT(*) * 100
                    END, 2
                ) AS successRate
            FROM order_info a
            LEFT JOIN pay_config_channel b ON a.pay_config_channel_id = b.id
            WHERE b.share_id = 1
            AND (#{startTime} IS NULL OR a.create_time >= #{startTime})
            <if test="platformIds != null and platformIds.size() > 0">
                AND a.platform_id IN
                <foreach collection="platformIds" item="pid" open="(" separator="," close=")">
                    #{pid}
                </foreach>
            </if>
            GROUP BY DATE(a.create_time), a.pay_config_channel_id, a.platform_id
            ORDER BY DATE(a.create_time) DESC
            </script>
            """)
    IPage<PayinSummaryRowDTO> selectPayinSummaryPage(
            IPage<PayinSummaryRowDTO> page,
            @Param("startTime")   String startTime,
            @Param("platformIds") List<Integer> platformIds);

    @Select("""
            SELECT
                a.platform_id AS platformId,
                COUNT(*) AS orderNum,
                SUM(CASE WHEN a.status = 1 THEN 1 ELSE 0 END) AS successCount,
                ROUND(
                    CASE WHEN COUNT(*) = 0 THEN 0
                    ELSE SUM(CASE WHEN a.status = 1 THEN 1 ELSE 0 END) / COUNT(*) * 100
                    END, 2
                ) AS successRate
            FROM order_info a
            WHERE a.create_time >= #{startTime}
            GROUP BY a.platform_id
            ORDER BY a.platform_id
            """)
    List<MerchantRateDTO> selectMerchantSuccessRates(@Param("startTime") String startTime);
}
