package com.letsvpn.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.letsvpn.pay.entity.PayPlatformInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 支付平台信息表 Mapper 接口
 * </p>
 *
 * @author (Your Name or Generator)
 * @since (Date)
 */
@Mapper
public interface PayPlatformInfoMapper extends BaseMapper<PayPlatformInfo> {

    @Select("SELECT a.platform_id FROM pay_platform_info a INNER JOIN shopline_shop_token b ON a.platform_no = b.shop_id")
    List<Integer> selectShoplinePlatformIds();
}