package com.letsvpn.pay.mapper.ext;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.letsvpn.common.core.dto.PayChannelPageRowDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExtPayChannelPageMapper {

    @Select("""
            <script>
            SELECT
                a.id,
                c.area_type AS areaType,
                a.title,
                c.url,
                a.title AS description,
                c.title AS configTitle,
                a.status,
                a.create_time AS createTime,
                a.pay_config_id AS configId,
                a.share_id AS shareId
            FROM pay_config_channel a
            LEFT JOIN pay_config_info c ON a.pay_config_id = c.id
            <where>
                <if test="id != null">AND a.id = #{id}</if>
                <if test="title != null and title != ''">AND a.title LIKE CONCAT('%', #{title}, '%')</if>
                <if test="status != null">AND a.status = #{status}</if>
                <if test="areaType != null">AND c.area_type = #{areaType}</if>
            </where>
            ORDER BY a.create_time DESC
            </script>
            """)
    IPage<PayChannelPageRowDTO> selectPayChannelPage(
            IPage<PayChannelPageRowDTO> page,
            @Param("id")       Long id,
            @Param("title")    String title,
            @Param("status")   Integer status,
            @Param("areaType") Integer areaType);
}
