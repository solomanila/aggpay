package com.letsvpn.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.letsvpn.admin.entity.MerchantBalanceLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface MerchantBalanceLogMapper extends BaseMapper<MerchantBalanceLog> {

    @Select("SELECT COALESCE(SUM(amount), 0) FROM merchant_balance_log " +
            "WHERE platform_id = #{platformId} AND op_type = 'WITHDRAW' AND currency = 'INR'")
    BigDecimal sumWithdrawByPlatform(@Param("platformId") Integer platformId);
}
