package com.letsvpn.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.letsvpn.pay.entity.PayConfigInfo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 支付配置信息表 Mapper 接口
 * </p>
 *
 * @author (Your Name or Generator)
 * @since (Date)
 */
@Mapper
public interface PayConfigInfoMapper extends BaseMapper<PayConfigInfo> {

    @Select("select distinct area_type from pay_config_info where area_type is not null")
    List<Integer> selectDistinctAreaTypes();

    @Select("select count(1) from pay_config_info")
    Long countActiveChannels();

    @Select("select id from pay_config_info")
    List<Long> selectAllIds();
}
