package com.letsvpn.pay.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.letsvpn.pay.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

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
}