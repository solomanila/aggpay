-- ============================================================
-- 商户扩展表设计（admin 数据库）
-- 关联关系：admin.merchant_op_config.platform_id
--           = aggpay.pay_platform_info.platform_id
-- ============================================================

-- ------------------------------------------------------------
-- 1. 商户运营配置（限额 / 风控 / 通知）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `merchant_op_config` (
  `id`                          bigint        NOT NULL AUTO_INCREMENT,
  `platform_id`                 int           NOT NULL COMMENT '对应 aggpay.pay_platform_info.platform_id',
  `email`                       varchar(128)  DEFAULT NULL  COMMENT '邮箱',
  `agent_id`                    bigint        DEFAULT NULL  COMMENT '代理商 ID',
  `remark`                      varchar(255)  DEFAULT NULL  COMMENT '备注',

  -- 用户收款限制
  `daily_pay_order_limit`       int           DEFAULT 5     COMMENT '用户每日收款订单数限制',

  -- 用户提现限制
  `daily_withdraw_count_limit`  int           DEFAULT 5     COMMENT '用户每日提现次数限制',
  `daily_withdraw_amount_limit` decimal(18,2) DEFAULT 50000.00 COMMENT '用户每日提款金额限制',

  -- 商户整体日限额
  `daily_pay_limit`             decimal(18,2) DEFAULT NULL  COMMENT '商户代收日限额（0=不限）',
  `daily_payout_limit`          decimal(18,2) DEFAULT NULL  COMMENT '商户代付日限额（0=不限）',

  -- 大额出款风控
  `large_payout_risk_enabled`   tinyint(1)    DEFAULT 0     COMMENT '大额单笔出款风控开关 0=OFF 1=ON',
  `large_payout_risk_amount`    decimal(18,2) DEFAULT NULL  COMMENT '大额单笔出款风控金额阈值',

  -- 通知
  `telegram_group_id`           varchar(64)   DEFAULT NULL  COMMENT 'Telegram 群 ID（负数为群组）',
  `settlement_notify`           tinyint(1)    DEFAULT 1     COMMENT '结算通知 0=OFF 1=ON',

  `created_at`                  datetime      DEFAULT CURRENT_TIMESTAMP,
  `updated_at`                  datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_id` (`platform_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='商户运营配置（限额/风控/通知），platform_id 与 aggpay.pay_platform_info 对应';


-- ------------------------------------------------------------
-- 2. 商户多币种余额
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `merchant_balance` (
  `id`          bigint        NOT NULL AUTO_INCREMENT,
  `platform_id` int           NOT NULL COMMENT '对应 aggpay.pay_platform_info.platform_id',
  `currency`    varchar(16)   NOT NULL COMMENT '货币代码 INR/RUB/USDT 等',
  `available`   decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '可用余额',
  `frozen`      decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '冻结余额',
  `updated_at`  datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_currency` (`platform_id`, `currency`),
  KEY `idx_platform_id` (`platform_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='商户多币种余额（可用 + 冻结）';


-- ------------------------------------------------------------
-- 3. 商户余额流水
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `merchant_balance_log` (
  `id`               bigint        NOT NULL AUTO_INCREMENT,
  `platform_id`      int           NOT NULL COMMENT '商户 ID',
  `currency`         varchar(16)   NOT NULL COMMENT '货币代码',
  `op_type`          varchar(16)   NOT NULL COMMENT 'RECHARGE/DEDUCT/FREEZE/UNFREEZE/WITHDRAW',
  `amount`           decimal(18,4) NOT NULL COMMENT '操作金额（正数）',
  `before_available` decimal(18,4) NOT NULL COMMENT '操作前可用余额',
  `after_available`  decimal(18,4) NOT NULL COMMENT '操作后可用余额',
  `before_frozen`    decimal(18,4) NOT NULL COMMENT '操作前冻结余额',
  `after_frozen`     decimal(18,4) NOT NULL COMMENT '操作后冻结余额',
  `remark`           varchar(255)  DEFAULT NULL COMMENT '备注',
  `operator_id`      bigint        DEFAULT NULL COMMENT '操作人（admin user id）',
  `created_at`       datetime      DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  KEY `idx_platform_currency` (`platform_id`, `currency`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='商户余额操作流水（充值/扣减/冻结/解冻/提现）';


-- ------------------------------------------------------------
-- 4. 商户通道配置（代收 & 代付路由）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `merchant_channel_config` (
  `id`                    bigint        NOT NULL AUTO_INCREMENT,
  `platform_id`           int           NOT NULL COMMENT '商户 ID',
  `pay_config_channel_id` bigint        NOT NULL COMMENT '通道 ID，对应 aggpay.pay_config_channel.id',
  `channel_type`          varchar(16)   NOT NULL DEFAULT 'PAYMENT' COMMENT 'PAYMENT=代收 PAYOUT=代付',

  -- 路由
  `daily_limit`           decimal(18,2) NOT NULL DEFAULT 0 COMMENT '该商户在此通道的日限额（0=不限）',
  `weight`                int           NOT NULL DEFAULT 1 COMMENT '路由权重',
  `enabled`               tinyint(1)   NOT NULL DEFAULT 0 COMMENT '是否启用 0=禁用 1=启用',

  -- 时间窗口
  `start_time`            varchar(8)    DEFAULT NULL COMMENT '生效开始时间 HH:mm',
  `end_time`              varchar(8)    DEFAULT NULL COMMENT '生效结束时间 HH:mm',

  -- 结算
  `settlement_cycle`      int           DEFAULT NULL COMMENT '结算周期（分钟）',
  `auto_settle`           tinyint(1)   DEFAULT 0    COMMENT '自动到账 0=否 1=是',

  -- 金额限制
  `min_amount`            decimal(18,2) DEFAULT 0    COMMENT '单笔最小金额',
  `max_amount`            decimal(18,2) DEFAULT NULL COMMENT '单笔最大金额（NULL=不限）',

  -- 支付方式（仅代收）
  `currency`              varchar(16)   DEFAULT NULL COMMENT '货币 INR/RUB 等',
  `pay_mode`              varchar(32)   DEFAULT NULL COMMENT '支付模式 WEB/APP/H5 等',
  `pay_page`              varchar(128)  DEFAULT NULL COMMENT '支付页面标识',

  -- 默认费率
  `fee_rate`              decimal(8,4)  NOT NULL DEFAULT 0.0000 COMMENT '费率（百分比，如 8.0 表示 8%）',
  `fee_fixed`             decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '固定手续费',

  -- 分段费率
  `tiered_rate_enabled`   tinyint(1)   DEFAULT 0 COMMENT '是否开启分段费率',
  `tiered_rate_config`    json          DEFAULT NULL COMMENT '分段费率配置 JSON',

  `created_at`            datetime      DEFAULT CURRENT_TIMESTAMP,
  `updated_at`            datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_channel` (`platform_id`, `pay_config_channel_id`),
  KEY `idx_platform_type_enabled` (`platform_id`, `channel_type`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='商户通道配置（路由权重/日限额/时间窗口/费率）';
