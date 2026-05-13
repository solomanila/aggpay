package com.letsvpn.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.letsvpn.admin.entity.PayChannelProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PayChannelProfileMapper extends BaseMapper<PayChannelProfile> {

    @Select("SELECT DISTINCT area_type FROM pay_channel_profile WHERE area_type IS NOT NULL AND area_type != '' ORDER BY area_type")
    List<String> selectDistinctAreaTypes();

    @Select("SELECT DISTINCT business_types FROM pay_channel_profile WHERE business_types IS NOT NULL AND business_types != 'null' AND business_types != '[]'")
    List<String> selectDistinctBusinessTypes();
}
