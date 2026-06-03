package com.letsvpn.common.core.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 对外商户查单响应 DTO，仅含可安全暴露的字段。
 * 不含：pay_config_id / pay_config_channel_id / other_order_id /
 *      settle_amount / create_ip / upi / extend1~3 / sync_status / on_line_id
 */
@Data
public class MerchantOrderQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderId;
    private String frontId;
    private Integer platformId;
    private Integer userId;
    private Integer gameId;
    private Integer status;
    private BigDecimal reqAmount;
    private BigDecimal realAmount;
    private Date payTime;
    private Date createTime;
    private Integer noticeStatus;
    private String remark;
}
