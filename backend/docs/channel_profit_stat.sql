-- 通道利润日统计表（admin schema）
-- 由 admin-service 定时任务每天凌晨 01:00 自动写入前一天各通道数据
-- system_amount 和 channel_fee_income 由调度器计算；其余字段留给人工录入

CREATE TABLE IF NOT EXISTS `channel_profit_stat` (
  `id`                   BIGINT        NOT NULL AUTO_INCREMENT             COMMENT '主键',
  `stat_date`            DATE          NOT NULL                            COMMENT '统计日期',
  `channel_id`           BIGINT        NOT NULL                            COMMENT 'pay_config_channel.id',
  `channel_name`         VARCHAR(128)  NOT NULL                            COMMENT '通道名称（快照）',
  `system_amount`        DECIMAL(18,4) NOT NULL DEFAULT 0                  COMMENT '系统计算金额：当日 status=1 的 real_amount 汇总',
  `channel_fee_income`   DECIMAL(18,4) NOT NULL DEFAULT 0                  COMMENT '通道费收入：Σ(商户金额 × merchant_channel_config.fee_rate)',
  `collection_amount`    DECIMAL(18,4)     NULL DEFAULT NULL               COMMENT '代收户金额（人工录入）',
  `dropped_order_income` DECIMAL(18,4)     NULL DEFAULT NULL               COMMENT '掉单收入（人工录入）',
  `channel_cost`         DECIMAL(18,4)     NULL DEFAULT NULL               COMMENT '通道成本（人工录入）',
  `other_cost`           DECIMAL(18,4)     NULL DEFAULT NULL               COMMENT '其他成本（人工录入）',
  `frozen_amount`        DECIMAL(18,4)     NULL DEFAULT NULL               COMMENT '冻结金额（人工录入）',
  `adjustment`           DECIMAL(18,4)     NULL DEFAULT NULL               COMMENT '调差（人工录入）',
  `profit`               DECIMAL(18,4)     NULL DEFAULT NULL               COMMENT '利润（人工录入）',
  `create_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  `update_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_channel` (`stat_date`, `channel_id`),
  KEY `idx_stat_date`  (`stat_date`),
  KEY `idx_channel_id` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道利润日统计';
