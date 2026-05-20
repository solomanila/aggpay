package com.letsvpn.pay.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.letsvpn.common.core.dto.OrderCallbackDTO;
import com.letsvpn.pay.entity.OrderCallback;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 订单回调记录表 Mapper 接口
 * </p>
 *
 * @author (Your Name or Generator)
 * @since (Date)
 */
@Mapper
public interface OrderCallbackMapper extends BaseMapper<OrderCallback> {

    @Select("SELECT b.order_id AS orderId, a.platform_no AS platformNo, " +
            "a.req_url AS reqUrl, a.param, a.status AS respCode, a.create_time AS createTime " +
            "FROM order_callback a " +
            "LEFT JOIN order_info b ON b.order_id = #{orderId} " +
            "WHERE a.param LIKE #{paramLike} " +
            "ORDER BY a.create_time DESC")
    List<OrderCallbackDTO> selectByOrderId(@Param("orderId") String orderId,
                                           @Param("paramLike") String paramLike);
}