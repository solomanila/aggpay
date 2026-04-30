-- Consolidated schema (<=30 tables) covering md requirements

CREATE TABLE IF NOT EXISTS `merchant_profile` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `merchant_code` varchar(64) NOT NULL UNIQUE,
  `platform_id` int NOT NULL,
  `name` varchar(128) NOT NULL,
  `region` varchar(32) DEFAULT NULL,
  `business_type` varchar(64) DEFAULT NULL,
  `tier` varchar(32) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ACTIVE',
  `risk_level` varchar(16) DEFAULT 'LOW',
  `owner_user_id` bigint DEFAULT NULL,
  `tags` json DEFAULT NULL,
  `contact_name` varchar(64) DEFAULT NULL,
  `contact_info` varchar(128) DEFAULT NULL,
  `extra` json DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户主档案 (覆盖 MerchantsList/Board/Spotlight)';

CREATE TABLE IF NOT EXISTS `merchant_metric_snapshot` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `merchant_id` bigint NOT NULL,
  `stat_time` datetime NOT NULL,
  `gmv` decimal(18,2) DEFAULT 0,
  `success_rate` decimal(5,2) DEFAULT 0,
  `refund_rate` decimal(5,2) DEFAULT 0,
  `retention7` decimal(5,2) DEFAULT 0,
  `retention14` decimal(5,2) DEFAULT 0,
  `retention30` decimal(5,2) DEFAULT 0,
  `risk_score` decimal(5,2) DEFAULT 0,
  `cost` decimal(18,2) DEFAULT 0,
  `gross_margin_pct` decimal(5,2) DEFAULT 0,
  UNIQUE KEY `uk_merchant_time` (`merchant_id`,`stat_time`),
  FOREIGN KEY (`merchant_id`) REFERENCES `merchant_profile`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户 KPI & 留存快照';

CREATE TABLE IF NOT EXISTS `merchant_engagement` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `merchant_id` bigint NOT NULL,
  `type` varchar(32) NOT NULL COMMENT 'TICKET|CAMPAIGN|ALERT|NOTICE',
  `title` varchar(255) NOT NULL,
  `content` varchar(512) DEFAULT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `severity` varchar(16) DEFAULT 'INFO',
  `status` varchar(32) DEFAULT 'OPEN',
  `metric_value` varchar(64) DEFAULT NULL,
  `action` varchar(128) DEFAULT NULL,
  `eta` varchar(64) DEFAULT NULL,
  `effective_time` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_merchant_type` (`merchant_id`,`type`,`status`),
  FOREIGN KEY (`merchant_id`) REFERENCES `merchant_profile`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户 Tickets/活动/风险/通知统一表';

CREATE TABLE IF NOT EXISTS `merchant_timeline` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `merchant_id` bigint NOT NULL,
  `event_time` datetime NOT NULL,
  `event_type` varchar(32) DEFAULT 'INFO',
  `title` varchar(128) NOT NULL,
  `detail` varchar(512) DEFAULT NULL,
  `operator_user_id` bigint DEFAULT NULL,
  KEY `idx_merchant_time` (`merchant_id`,`event_time`),
  FOREIGN KEY (`merchant_id`) REFERENCES `merchant_profile`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户级时间线 (board/insights)';

CREATE TABLE IF NOT EXISTS `agent_profile` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `agent_code` varchar(64) NOT NULL UNIQUE,
  `name` varchar(128) NOT NULL,
  `region` varchar(32) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ENABLED',
  `quota_total` decimal(18,2) DEFAULT 0,
  `quota_used` decimal(18,2) DEFAULT 0,
  `balance` decimal(18,2) DEFAULT 0,
  `owner_user_id` bigint DEFAULT NULL,
  `contact` varchar(128) DEFAULT NULL,
  `tags` json DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理商档案 + 资金概览';

CREATE TABLE IF NOT EXISTS `agent_channel_binding` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `agent_id` bigint NOT NULL,
  `channel_id` bigint NOT NULL,
  `quota_percent` decimal(5,2) DEFAULT 0,
  `status` varchar(16) DEFAULT 'ENABLED',
  `fallback_channel_id` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_agent_channel` (`agent_id`,`channel_id`),
  FOREIGN KEY (`agent_id`) REFERENCES `agent_profile`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理商与通道绑定/配额';

CREATE TABLE IF NOT EXISTS `bank_supplier` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `supplier_code` varchar(64) NOT NULL UNIQUE,
  `name` varchar(128) NOT NULL,
  `country` varchar(32) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ACTIVE',
  `owner_user_id` bigint DEFAULT NULL,
  `contact` varchar(128) DEFAULT NULL,
  `tags` json DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行供户 (覆盖供应商/文档/维护)';

CREATE TABLE IF NOT EXISTS `bank_account` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `supplier_id` bigint NOT NULL,
  `account_code` varchar(64) NOT NULL UNIQUE,
  `bank_name` varchar(128) NOT NULL,
  `branch` varchar(128) DEFAULT NULL,
  `account_no` varchar(64) NOT NULL,
  `currency` varchar(8) NOT NULL,
  `status` varchar(16) DEFAULT 'ENABLED',
  `balance` decimal(18,2) DEFAULT 0,
  `daily_limit` decimal(18,2) DEFAULT NULL,
  `limit_usage_pct` decimal(5,2) DEFAULT 0,
  `risk_level` varchar(16) DEFAULT 'LOW',
  `platform_scope` varchar(64) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `tags` json DEFAULT NULL,
  `extra` json DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`supplier_id`) REFERENCES `bank_supplier`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行公户 (含 mapping/渠道绑定信息)';

CREATE TABLE IF NOT EXISTS `bank_account_metric_snapshot` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `stat_time` datetime NOT NULL,
  `inflow_amount` decimal(18,2) DEFAULT 0,
  `outflow_amount` decimal(18,2) DEFAULT 0,
  `success_rate` decimal(5,2) DEFAULT 0,
  `queue_depth` int DEFAULT 0,
  `alert_level` varchar(16) DEFAULT 'NORMAL',
  UNIQUE KEY `uk_account_time` (`account_id`,`stat_time`),
  FOREIGN KEY (`account_id`) REFERENCES `bank_account`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行账户实时指标 (Realtime Board/Monitor)';

CREATE TABLE IF NOT EXISTS `bank_account_event` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `event_time` datetime NOT NULL,
  `event_type` varchar(32) NOT NULL COMMENT 'LEDGER|MAINTENANCE|ALERT|MAPPING',
  `amount` decimal(18,2) DEFAULT NULL,
  `balance_after` decimal(18,2) DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `operator_user_id` bigint DEFAULT NULL,
  `status` varchar(16) DEFAULT NULL,
  KEY `idx_account_time` (`account_id`,`event_time`),
  FOREIGN KEY (`account_id`) REFERENCES `bank_account`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行账户事件/账本/维护合并表';

CREATE TABLE IF NOT EXISTS `bank_bookkeeping_entry` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `entry_type` varchar(16) NOT NULL COMMENT 'BOOK|ADJUST|CHARGE',
  `amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `direction` varchar(8) NOT NULL COMMENT 'DEBIT|CREDIT',
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `approval_stage` varchar(32) DEFAULT 'FINANCE',
  `approval_status` varchar(16) DEFAULT 'PENDING',
  `approval_comment` varchar(255) DEFAULT NULL,
  `evidences` json DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`account_id`) REFERENCES `bank_account`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记账条目 + 审批信息';

CREATE TABLE IF NOT EXISTS `pay_entity_profile` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `entity_code` varchar(64) NOT NULL UNIQUE,
  `name` varchar(128) NOT NULL,
  `country` varchar(32) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ACTIVE',
  `license_no` varchar(64) DEFAULT NULL,
  `compliance_score` decimal(5,2) DEFAULT 0,
  `documents` json DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付主体档案 (实体/证照/文档/审批)';

CREATE TABLE IF NOT EXISTS `pay_channel_profile` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `channel_code` varchar(64) NOT NULL UNIQUE,
  `name` varchar(128) NOT NULL,
  `area_type` varchar(32) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ACTIVE',
  `entity_id` bigint DEFAULT NULL,
  `business_types` json DEFAULT NULL,
  `fee_rate` decimal(8,4) DEFAULT NULL,
  `cost_rate` decimal(8,4) DEFAULT NULL,
  `routing_weight` int DEFAULT 0,
  `limit_config` json DEFAULT NULL,
  `maintenance_window` json DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `tags` json DEFAULT NULL,
  `extra` json DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`entity_id`) REFERENCES `pay_entity_profile`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付通道档案 (限流/维护/路由/利润)';

CREATE TABLE IF NOT EXISTS `pay_channel_operation` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `channel_id` bigint NOT NULL,
  `event_time` datetime NOT NULL,
  `event_type` varchar(32) NOT NULL COMMENT 'ERROR|FALLBACK|ROLLBACK|THROTTLE|PLAYBOOK|MAINTENANCE',
  `severity` varchar(16) DEFAULT 'INFO',
  `status` varchar(16) DEFAULT 'OPEN',
  `metric_value` varchar(64) DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `action_plan` varchar(255) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  KEY `idx_channel_time` (`channel_id`,`event_time`),
  FOREIGN KEY (`channel_id`) REFERENCES `pay_channel_profile`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付通道事件 (错误/兜底/回滚/演练)';

CREATE TABLE IF NOT EXISTS `pay_channel_limit_plan` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `channel_id` bigint NOT NULL,
  `plan_type` varchar(32) NOT NULL COMMENT 'STATIC|DYNAMIC|EMERGENCY',
  `limit_value` decimal(18,2) DEFAULT NULL,
  `window_minutes` int DEFAULT NULL,
  `threshold` decimal(18,2) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ACTIVE',
  `effective_from` datetime DEFAULT NULL,
  `effective_to` datetime DEFAULT NULL,
  `history` json DEFAULT NULL,
  FOREIGN KEY (`channel_id`) REFERENCES `pay_channel_profile`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限流/功率/配置历史';

CREATE TABLE IF NOT EXISTS `pay_reconcile_file` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `channel_id` bigint NOT NULL,
  `file_date` date NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `diff_count` int DEFAULT 0,
  `action` varchar(255) DEFAULT NULL,
  `notes` json DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`channel_id`) REFERENCES `pay_channel_profile`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账文件&处理动作';

CREATE TABLE IF NOT EXISTS `payout_batch` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `batch_no` varchar(64) NOT NULL UNIQUE,
  `type` varchar(32) DEFAULT 'PAYMENT',
  `status` varchar(16) DEFAULT 'PENDING',
  `total_amount` decimal(18,2) DEFAULT 0,
  `total_count` int DEFAULT 0,
  `success_count` int DEFAULT 0,
  `failed_count` int DEFAULT 0,
  `payload` json DEFAULT NULL COMMENT '详情列表',
  `approval_flow` json DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量代付/回滚任务 (含审批/详情 JSON)';

CREATE TABLE IF NOT EXISTS `payout_queue_event` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `queue_code` varchar(64) NOT NULL,
  `event_time` datetime NOT NULL,
  `event_type` varchar(32) NOT NULL COMMENT 'SNAPSHOT|NOTICE|ALERT',
  `pending_count` int DEFAULT 0,
  `throughput` int DEFAULT 0,
  `latency_ms` int DEFAULT 0,
  `content` json DEFAULT NULL,
  KEY `idx_queue_time` (`queue_code`,`event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代付队列快照/通知';

CREATE TABLE IF NOT EXISTS `payout_rollback_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `reason` varchar(255) DEFAULT NULL,
  `timeline` json DEFAULT NULL COMMENT '审批/动作时间线',
  `owner_user_id` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代付回滚/审批/时间线';

CREATE TABLE IF NOT EXISTS `order_operation_timeline` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `order_type` varchar(16) NOT NULL COMMENT 'COLLECTION|PAYOUT',
  `event_time` datetime NOT NULL,
  `event_type` varchar(32) NOT NULL COMMENT 'QUERY|CALLBACK|RISK|DISPUTE',
  `content` json DEFAULT NULL,
  `operator_user_id` bigint DEFAULT NULL,
  KEY `idx_order_time` (`order_id`,`event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单相关时间线 (查单/催办/风控)';

CREATE TABLE IF NOT EXISTS `finance_billing` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `billing_no` varchar(64) NOT NULL UNIQUE,
  `billing_type` varchar(32) NOT NULL COMMENT 'CLOUD|THIRD_PARTY|OPS',
  `cycle_start` date NOT NULL,
  `cycle_end` date NOT NULL,
  `amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `status` varchar(16) DEFAULT 'PENDING',
  `items` json DEFAULT NULL,
  `approval_flow` json DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统账单/审批';

CREATE TABLE IF NOT EXISTS `finance_settlement` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `settle_type` varchar(32) NOT NULL COMMENT 'SYSTEM_TOPUP|WITHDRAW|AGENT',
  `entity_id` bigint DEFAULT NULL,
  `amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `status` varchar(16) DEFAULT 'PENDING',
  `approval_flow` json DEFAULT NULL,
  `evidence` json DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现/系统充值/代理结算统一表';

CREATE TABLE IF NOT EXISTS `download_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_code` varchar(64) NOT NULL UNIQUE,
  `task_name` varchar(128) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `params` json DEFAULT NULL,
  `file_path` varchar(255) DEFAULT NULL,
  `expire_at` datetime DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `audit_log` json DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='下载任务与日志';

CREATE TABLE IF NOT EXISTS `ops_batch_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_code` varchar(64) NOT NULL UNIQUE,
  `task_name` varchar(128) NOT NULL,
  `task_type` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `payload` json DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `approval_flow` json DEFAULT NULL,
  `result_summary` json DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量操作/审批/处理';

CREATE TABLE IF NOT EXISTS `ops_notification` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `channel` varchar(32) NOT NULL COMMENT 'TELEGRAM|SMS|VPS|EMAIL',
  `target` varchar(128) NOT NULL,
  `title` varchar(128) NOT NULL,
  `content` varchar(512) DEFAULT NULL,
  `level` varchar(16) DEFAULT 'INFO',
  `status` varchar(16) DEFAULT 'PENDING',
  `meta` json DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `sent_at` datetime DEFAULT NULL,
  `last_error` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Telegram/SMS/VPS/Email 通知汇总';

CREATE TABLE IF NOT EXISTS `fund_snapshot` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `stat_time` datetime NOT NULL,
  `platform_id` int DEFAULT NULL,
  `balance_in` decimal(18,2) DEFAULT 0,
  `balance_out` decimal(18,2) DEFAULT 0,
  `in_transit` decimal(18,2) DEFAULT 0,
  `pipeline` json DEFAULT NULL,
  UNIQUE KEY `uk_platform_time` (`platform_id`,`stat_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金总览/流水/transfer 数据源';

CREATE TABLE IF NOT EXISTS `system_setting` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `setting_key` varchar(128) NOT NULL UNIQUE,
  `setting_name` varchar(128) NOT NULL,
  `value` text NOT NULL,
  `value_type` varchar(16) DEFAULT 'STRING',
  `category` varchar(64) DEFAULT 'GENERAL',
  `description` varchar(255) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ACTIVE',
  `gray_scope` varchar(128) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';

CREATE TABLE IF NOT EXISTS `system_setting_audit` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `setting_id` bigint NOT NULL,
  `version` int NOT NULL,
  `value_snapshot` text NOT NULL,
  `change_type` varchar(32) NOT NULL,
  `operator_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`setting_id`) REFERENCES `system_setting`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置审计';

CREATE TABLE IF NOT EXISTS `system_feature_flag` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `flag_key` varchar(128) NOT NULL UNIQUE,
  `description` varchar(255) DEFAULT NULL,
  `enabled` tinyint(1) DEFAULT 0,
  `scope` varchar(128) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灰度开关';

 CREATE TABLE IF NOT EXISTS `system_user_auth` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT,
    `account` varchar(64) NOT NULL UNIQUE COMMENT '登录账号',
    `name` varchar(64) NOT NULL,
    `email` varchar(128) DEFAULT NULL,
    `mobile` varchar(32) DEFAULT NULL,
    `status` varchar(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE|FROZEN|DISABLED|RISK|REVIEW',
    `risk_level` varchar(16) DEFAULT 'LOW',
    `owner_user_id` bigint DEFAULT NULL COMMENT '直属上级/负责人',
    `tags` json DEFAULT NULL,
    `last_login_at` datetime DEFAULT NULL,
    `last_login_ip` varchar(64) DEFAULT NULL,
    `password_hash` varchar(255) NOT NULL COMMENT 'BCrypt/Argon2 哈希',
    `password_salt` varchar(64) DEFAULT NULL,
    `password_algo` varchar(32) DEFAULT 'bcrypt',
    `password_updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `force_reset` tinyint(1) DEFAULT 0 COMMENT '1=下次登录必须改密',
    `google_secret` varchar(64) DEFAULT NULL COMMENT 'Google Authenticator 密钥',
    `google_enabled` tinyint(1) DEFAULT 0 COMMENT '1=已开启 MFA',
    `google_last_verified_at` datetime DEFAULT NULL,
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台用户信息 + 密码 & Google MFA';
