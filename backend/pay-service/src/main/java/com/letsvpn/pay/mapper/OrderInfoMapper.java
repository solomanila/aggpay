package com.letsvpn.pay.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.letsvpn.pay.entity.OrderInfo;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 订单信息表 Mapper 接口
 * </p>
 *
 * @author (Your Name or Generator)
 * @since (Date)
 */
@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    @Select("SELECT COALESCE(SUM(req_amount), 0) FROM order_info " +
            "WHERE platform_id = #{platformId} AND pay_config_channel_id = #{channelId} " +
            "AND create_time >= CURDATE()")
    BigDecimal sumTodayAmount(@Param("platformId") Integer platformId,
                              @Param("channelId") Long channelId);

    @Select("SELECT COALESCE(SUM(real_amount), 0) FROM order_info " +
            "WHERE status = 1 AND pay_config_channel_id = #{channelId} " +
            "AND create_time >= #{startTime} AND on_line_id IS NULL")
    BigDecimal sumRealAmountByChannelSince(@Param("channelId") Long channelId,
                                           @Param("startTime") Date startTime);

    @Update("UPDATE order_info SET on_line_id = #{billId} " +
            "WHERE pay_config_channel_id = #{channelId} AND create_time >= #{startTime} " +
            "AND on_line_id IS NULL")
    void updateOnlineIdByChannel(@Param("channelId") Long channelId,
                                 @Param("startTime") Date startTime,
                                 @Param("billId") Long billId);

    @Select("SELECT COALESCE(SUM(a.real_amount), 0) FROM order_info a " +
            "LEFT JOIN pay_config_info b ON a.pay_config_id = b.id " +
            "WHERE a.status = 1 AND a.platform_id = #{platformId} AND b.area_type = 1")
    BigDecimal sumTotalIncomeByPlatform(@Param("platformId") Integer platformId);

    @Select("SELECT DISTINCT platform_id FROM order_info WHERE create_time >= CURDATE() AND platform_id IS NOT NULL")
    List<Integer> selectTodayDistinctPlatformIds();

    @Update("UPDATE order_info SET other_order_id = #{otherOrderId} WHERE order_id = #{orderId}")
    int updateOtherOrderId(@Param("orderId") String orderId,
                           @Param("otherOrderId") String otherOrderId);
}