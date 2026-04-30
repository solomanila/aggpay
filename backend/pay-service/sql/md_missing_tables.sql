-- md/agent/AgentsChannelsView.md:agent_channel_binding
CREATE TABLE IF NOT EXISTS `agent_channel_binding` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `agent_id` bigint NOT NULL,
  `channel_id` bigint NOT NULL,
  `quota_percent` decimal(5,2) DEFAULT 0,
  `status` varchar(16) DEFAULT 'ENABLED',
  `fallback_channel_id` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_agent_channel` (`agent_id`,`channel_id`),
  FOREIGN KEY (`agent_id`) REFERENCES `agent_profile`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`channel_id`) REFERENCES `pay_config_channel`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理商渠道绑定';

-- md/agent/AgentsChannelsView.md:agent_notice
CREATE TABLE IF NOT EXISTS `agent_notice` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `agent_id` bigint DEFAULT NULL,
  `title` varchar(128) NOT NULL,
  `content` varchar(255) DEFAULT NULL,
  `level` varchar(16) DEFAULT 'INFO',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`agent_id`) REFERENCES `agent_profile`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理商提醒';

-- md/agent/AgentsChannelsView.md:agent_profile
CREATE TABLE IF NOT EXISTS `agent_profile` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `agent_code` varchar(64) NOT NULL UNIQUE,
  `name` varchar(128) NOT NULL,
  `region` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'ENABLED' COMMENT 'ENABLED|MAINTENANCE|DISABLED',
  `quota_amount` decimal(18,2) DEFAULT 0 COMMENT '可用配额',
  `quota_used` decimal(18,2) DEFAULT 0,
  `owner_user_id` bigint DEFAULT NULL,
  `contact` varchar(64) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理商档案';

-- md/agent/AgentsListView.md:agent_finance_snapshot
CREATE TABLE IF NOT EXISTS `agent_finance_snapshot` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `agent_id` bigint NOT NULL,
  `stat_date` date NOT NULL,
  `balance` decimal(18,2) DEFAULT 0,
  `quota_used` decimal(18,2) DEFAULT 0,
  `payout_amount` decimal(18,2) DEFAULT 0,
  `fee_amount` decimal(18,2) DEFAULT 0,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_agent_date` (`agent_id`,`stat_date`),
  FOREIGN KEY (`agent_id`) REFERENCES `agent_profile`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理商资金快照';

-- md/bankaccounts/BankAccountsView.md:bank_account
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
  `owner_user_id` bigint DEFAULT NULL,
  `risk_level` varchar(16) DEFAULT 'LOW',
  `platform_scope` varchar(64) DEFAULT NULL COMMENT '可服务的平台/区域',
  `tags` varchar(128) DEFAULT NULL,
  `last_reconcile_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`supplier_id`) REFERENCES `bank_supplier`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行公户信息';

-- md/bankaccounts/BankAccountsView.md:bank_account_maintenance
CREATE TABLE IF NOT EXISTS `bank_account_maintenance` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `window_start` datetime NOT NULL,
  `window_end` datetime NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'PLANNED',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`account_id`) REFERENCES `bank_account`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行账户维护';

-- md/bankaccounts/BankBookkeepingView.md:bank_bookkeeping_approval
CREATE TABLE IF NOT EXISTS `bank_bookkeeping_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `entry_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL COMMENT 'FINANCE|RISK|OPS',
  `status` varchar(16) DEFAULT 'PENDING' COMMENT 'PENDING|APPROVED|REJECTED',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`entry_id`) REFERENCES `bank_bookkeeping_entry`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记账审批流';

-- md/bankaccounts/BankBookkeepingView.md:bank_bookkeeping_entry
CREATE TABLE IF NOT EXISTS `bank_bookkeeping_entry` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `entry_type` varchar(16) NOT NULL COMMENT 'BOOK|ADJUST|REVERSAL',
  `amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) NOT NULL,
  `tag` varchar(32) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'POSTED' COMMENT 'POSTED|PENDING_REVIEW|REJECTED',
  `owner_user_id` bigint DEFAULT NULL,
  `related_ledger_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`account_id`) REFERENCES `bank_account`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`related_ledger_id`) REFERENCES `bank_account_ledger`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行记账流水';

-- md/bankaccounts/BankChannelSettingsView.md:bank_account_channel_binding
CREATE TABLE IF NOT EXISTS `bank_account_channel_binding` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `pay_config_channel_id` bigint NOT NULL,
  `weight_percent` decimal(5,2) DEFAULT 0,
  `status` varchar(16) DEFAULT 'ENABLED',
  `fallback_channel_id` bigint DEFAULT NULL,
  `priority` tinyint DEFAULT 1,
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_account_channel` (`account_id`,`pay_config_channel_id`),
  FOREIGN KEY (`account_id`) REFERENCES `bank_account`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`pay_config_channel_id`) REFERENCES `pay_config_channel`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行账户通道绑定关系';

-- md/bankaccounts/BankChannelSettingsView.md:bank_account_channel_plan
CREATE TABLE IF NOT EXISTS `bank_account_channel_plan` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `binding_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `progress` tinyint DEFAULT 0,
  `eta` datetime DEFAULT NULL,
  `status` varchar(16) DEFAULT 'OPEN',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`binding_id`) REFERENCES `bank_account_channel_binding`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绑定调优计划';

-- md/bankaccounts/BankLedgerView.md:bank_account_ledger
CREATE TABLE IF NOT EXISTS `bank_account_ledger` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `ledger_no` varchar(64) NOT NULL UNIQUE,
  `direction` varchar(8) NOT NULL COMMENT 'CREDIT|DEBIT',
  `amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) NOT NULL,
  `status` varchar(16) DEFAULT 'POSTED' COMMENT 'POSTED|IN_TRANSIT|PENDING_REVIEW|ERROR',
  `tag` varchar(32) DEFAULT NULL COMMENT 'UPI/WALLET/PIX 等',
  `order_id` bigint DEFAULT NULL,
  `pay_config_channel_id` bigint DEFAULT NULL,
  `ref_no` varchar(128) DEFAULT NULL COMMENT '银行流水号',
  `remark` varchar(255) DEFAULT NULL,
  `occur_time` datetime NOT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`account_id`) REFERENCES `bank_account`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`pay_config_channel_id`) REFERENCES `pay_config_channel`(`id`),
  KEY `idx_account_time` (`account_id`,`occur_time`),
  KEY `idx_status_time` (`status`,`occur_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行账户流水';

-- md/bankaccounts/BankLedgerView.md:bank_account_ledger_timeline
CREATE TABLE IF NOT EXISTS `bank_account_ledger_timeline` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `ledger_id` bigint NOT NULL,
  `event_time` datetime NOT NULL,
  `event` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `data` json DEFAULT NULL,
  FOREIGN KEY (`ledger_id`) REFERENCES `bank_account_ledger`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水处理时间线';

-- md/bankaccounts/BankMappingView.md:bank_mapping_approval
CREATE TABLE IF NOT EXISTS `bank_mapping_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `template_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL COMMENT 'PLATFORM|OPS|RISK',
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`template_id`) REFERENCES `bank_mapping_template`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='映射模板审批';

-- md/bankaccounts/BankMappingView.md:bank_mapping_field
CREATE TABLE IF NOT EXISTS `bank_mapping_field` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `template_id` bigint NOT NULL,
  `field_name` varchar(64) NOT NULL,
  `source_field` varchar(64) NOT NULL,
  `transform_rule` varchar(255) DEFAULT NULL COMMENT 'JSONPath/表达式',
  `required` tinyint DEFAULT 1,
  `data_type` varchar(32) DEFAULT 'STRING',
  `remark` varchar(255) DEFAULT NULL,
  FOREIGN KEY (`template_id`) REFERENCES `bank_mapping_template`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行字段映射详情';

-- md/bankaccounts/BankMappingView.md:bank_mapping_template
CREATE TABLE IF NOT EXISTS `bank_mapping_template` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `bank_name` varchar(64) NOT NULL,
  `business_type` varchar(32) NOT NULL COMMENT 'UPI/WALLET/PIX 等',
  `version` varchar(16) NOT NULL,
  `status` varchar(16) DEFAULT 'ENABLED' COMMENT 'ENABLED|PENDING|DISABLED',
  `owner_user_id` bigint DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `file_url` varchar(512) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_bank_type_version` (`bank_name`,`business_type`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行字段映射模板';

-- md/bankaccounts/BankRealtimeBoardView.md:bank_account_alert
CREATE TABLE IF NOT EXISTS `bank_account_alert` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `severity` varchar(16) NOT NULL COMMENT 'LOW/MEDIUM/HIGH',
  `action` varchar(64) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'OPEN',
  `triggered_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `acked_by` bigint DEFAULT NULL,
  `acked_at` datetime DEFAULT NULL,
  FOREIGN KEY (`account_id`) REFERENCES `bank_account`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公户告警';

-- md/bankaccounts/BankRealtimeBoardView.md:bank_account_metric_snapshot
CREATE TABLE IF NOT EXISTS `bank_account_metric_snapshot` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `snapshot_time` datetime NOT NULL,
  `balance` decimal(18,2) NOT NULL,
  `inflow_rate` decimal(18,2) DEFAULT 0 COMMENT '单位:金额/分',
  `outflow_rate` decimal(18,2) DEFAULT 0,
  `txn_per_min` int DEFAULT 0,
  `limit_usage_pct` decimal(5,2) DEFAULT 0,
  `risk_level` varchar(16) DEFAULT 'LOW',
  KEY `idx_account_time` (`account_id`,`snapshot_time` DESC),
  FOREIGN KEY (`account_id`) REFERENCES `bank_account`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行账户实时快照';

-- md/bankaccounts/BankRealtimeBoardView.md:bank_account_timeline
CREATE TABLE IF NOT EXISTS `bank_account_timeline` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint DEFAULT NULL,
  `event_time` datetime NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `event_type` varchar(16) DEFAULT 'OPS',
  `data` json DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公户时间线';

-- md/bankaccounts/BankSuppliersView.md:bank_supplier
CREATE TABLE IF NOT EXISTS `bank_supplier` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `supplier_code` varchar(64) NOT NULL UNIQUE,
  `name` varchar(128) NOT NULL,
  `country` varchar(32) NOT NULL,
  `region` varchar(32) DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/MAINTENANCE/INACTIVE',
  `coverage_services` varchar(128) DEFAULT NULL COMMENT 'UPI/NEFT/TED 等',
  `license_no` varchar(64) DEFAULT NULL,
  `kyc_status` varchar(16) DEFAULT 'PENDING',
  `account_count` int DEFAULT 0,
  `owner_user_id` bigint DEFAULT NULL,
  `contact_name` varchar(64) DEFAULT NULL,
  `contact_email` varchar(128) DEFAULT NULL,
  `contact_phone` varchar(32) DEFAULT NULL,
  `notes` varchar(512) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行供应商档案';

-- md/bankaccounts/BankSuppliersView.md:bank_supplier_document
CREATE TABLE IF NOT EXISTS `bank_supplier_document` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `supplier_id` bigint NOT NULL,
  `doc_type` varchar(32) NOT NULL COMMENT 'LICENSE|CONTRACT|AML',
  `title` varchar(128) NOT NULL,
  `file_url` varchar(512) NOT NULL,
  `status` varchar(16) DEFAULT 'VALID',
  `uploaded_by` bigint DEFAULT NULL,
  `uploaded_at` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_supplier_doc` (`supplier_id`,`doc_type`),
  CONSTRAINT `fk_supplier_document` FOREIGN KEY (`supplier_id`) REFERENCES `bank_supplier`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商证照记录';

-- md/bankaccounts/BankSuppliersView.md:bank_supplier_maintenance
CREATE TABLE IF NOT EXISTS `bank_supplier_maintenance` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `supplier_id` bigint NOT NULL,
  `window_start` datetime NOT NULL,
  `window_end` datetime NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'PLANNED',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_supplier_status` (`supplier_id`,`status`),
  CONSTRAINT `fk_supplier_maintenance` FOREIGN KEY (`supplier_id`) REFERENCES `bank_supplier`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商维护计划';

-- md/dashboard/DashboardAgentLogView.md:agent_approval_flow
CREATE TABLE IF NOT EXISTS `agent_approval_flow` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `agent_code` varchar(64) NOT NULL,
  `platform_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `stage` varchar(64) NOT NULL,
  `owner` varchar(64) NOT NULL,
  `status` varchar(32) DEFAULT 'PENDING',
  `eta_text` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardAgentLogView.md:agent_insight
CREATE TABLE IF NOT EXISTS `agent_insight` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `content` varchar(512) NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardAgentLogView.md:agent_operation_log
CREATE TABLE IF NOT EXISTS `agent_operation_log` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `agent_code` varchar(64) NOT NULL,
  `platform_id` int NOT NULL,
  `op_type` varchar(32) NOT NULL COMMENT '充值/限额/风控/审批',
  `amount_text` varchar(128) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `operator` varchar(64) NOT NULL,
  `occur_time` datetime NOT NULL,
  `extra` json DEFAULT NULL,
  KEY `idx_agent_op_time` (`agent_code`,`occur_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardBankIdView.md:bank_id_session_metric
CREATE TABLE IF NOT EXISTS `bank_id_session_metric` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `merchant_id` int NOT NULL,
  `region` varchar(64) NOT NULL,
  `otp_success` decimal(5,2) NOT NULL,
  `auth_success` decimal(5,2) NOT NULL,
  `fallback_enabled` tinyint NOT NULL DEFAULT 0,
  `snapshot_time` datetime NOT NULL,
  KEY `idx_bankid_merchant_time` (`merchant_id`,`snapshot_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardBankIdView.md:bank_id_task
CREATE TABLE IF NOT EXISTS `bank_id_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `owner` varchar(64) NOT NULL,
  `progress` tinyint DEFAULT 0,
  `status` varchar(32) DEFAULT 'RUNNING',
  `eta_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardBankMonitorView.md:bank_monitor_automation
CREATE TABLE IF NOT EXISTS `bank_monitor_automation` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `owner` varchar(64) NOT NULL,
  `progress` tinyint DEFAULT 0,
  `eta_time` datetime DEFAULT NULL,
  `status` varchar(32) DEFAULT 'RUNNING'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardBankMonitorView.md:bank_monitor_bank
CREATE TABLE IF NOT EXISTS `bank_monitor_bank` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `bank_code` varchar(32) UNIQUE NOT NULL,
  `bank_name` varchar(128) NOT NULL,
  `region` varchar(64) NOT NULL,
  `platform_id` int NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'green',
  `latency_ms` int DEFAULT NULL,
  `incident_count` int DEFAULT 0,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardBankMonitorView.md:bank_monitor_maintenance
CREATE TABLE IF NOT EXISTS `bank_monitor_maintenance` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `bank_code` varchar(32) NOT NULL,
  `window_text` varchar(128) NOT NULL,
  `note` varchar(255) DEFAULT NULL,
  `platform_id` int NOT NULL,
  `status` varchar(32) DEFAULT 'PLANNED',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardBankMonitorView.md:bank_monitor_threshold
CREATE TABLE IF NOT EXISTS `bank_monitor_threshold` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `bank_code` varchar(32) NOT NULL,
  `metric` varchar(32) NOT NULL,
  `value` varchar(64) NOT NULL,
  `action` varchar(128) DEFAULT NULL,
  `platform_id` int NOT NULL,
  UNIQUE KEY `uniq_bank_metric` (`bank_code`,`metric`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardBankTradeView.md:bank_trade_integration
CREATE TABLE IF NOT EXISTS `bank_trade_integration` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `owner` varchar(64) NOT NULL,
  `progress` tinyint DEFAULT 0,
  `eta` varchar(64) DEFAULT NULL,
  `status` varchar(32) DEFAULT 'RUNNING',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardBankTradeView.md:bank_trade_queue_metric
CREATE TABLE IF NOT EXISTS `bank_trade_queue_metric` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `queue_name` varchar(64) NOT NULL,
  `platform_id` int NOT NULL,
  `depth` int NOT NULL,
  `wait_ms` int NOT NULL,
  `snapshot_time` datetime NOT NULL,
  KEY `idx_bt_queue_time` (`queue_name`,`snapshot_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardBankTradeView.md:bank_trade_service_metric
CREATE TABLE IF NOT EXISTS `bank_trade_service_metric` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `service_name` varchar(64) NOT NULL,
  `platform_id` int NOT NULL,
  `status` varchar(16) NOT NULL,
  `latency_ms` int NOT NULL,
  `success_rate` decimal(5,2) NOT NULL,
  `snapshot_time` datetime NOT NULL,
  KEY `idx_bt_service_time` (`service_name`,`snapshot_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardChannelView.md:channel_reroute_plan
CREATE TABLE IF NOT EXISTS `channel_reroute_plan` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `channel_id` bigint NOT NULL COMMENT 'pay_config_channel.id',
  `title` varchar(255) NOT NULL,
  `detail` varchar(512) DEFAULT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `eta_time` datetime DEFAULT NULL,
  `status` varchar(32) DEFAULT 'PENDING',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_reroute_channel` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardChannelView.md:ops_checklist_item
CREATE TABLE IF NOT EXISTS `ops_checklist_item` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `category` varchar(32) DEFAULT 'CHANNEL',
  `title` varchar(255) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `due_time` datetime DEFAULT NULL,
  `status` varchar(32) DEFAULT 'OPEN',
  `extra` json DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_checklist_platform_status` (`platform_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardFundsView.md:fund_balance_snapshot
CREATE TABLE IF NOT EXISTS `fund_balance_snapshot` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `currency` varchar(8) NOT NULL,
  `balance_amount` decimal(20,4) NOT NULL,
  `utilization_percent` tinyint DEFAULT 0,
  `in_transit_amount` decimal(20,4) DEFAULT 0,
  `snapshot_time` datetime NOT NULL,
  KEY `idx_balance_platform_currency` (`platform_id`,`currency`,`snapshot_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardFundsView.md:fund_pipeline_item
CREATE TABLE IF NOT EXISTS `fund_pipeline_item` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `label` varchar(64) NOT NULL,
  `items` json NOT NULL,
  `status` varchar(32) DEFAULT 'ACTIVE',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardFundsView.md:fund_transfer_task
CREATE TABLE IF NOT EXISTS `fund_transfer_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `owner` varchar(64) NOT NULL,
  `progress` tinyint DEFAULT 0,
  `eta_time` datetime DEFAULT NULL,
  `status` varchar(32) DEFAULT 'PENDING',
  `from_currency` varchar(8) DEFAULT NULL,
  `to_currency` varchar(8) DEFAULT NULL,
  `amount` decimal(20,4) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_transfer_platform_status` (`platform_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardMerchantView.md:merchant_campaign
CREATE TABLE IF NOT EXISTS `merchant_campaign` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `name` varchar(255) NOT NULL,
  `lift_percent` decimal(6,2) DEFAULT NULL,
  `status` varchar(32) DEFAULT 'ONGOING',
  `effective_time` datetime DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardMerchantView.md:merchant_risk_signal
CREATE TABLE IF NOT EXISTS `merchant_risk_signal` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `merchant_id` int NOT NULL,
  `platform_id` int NOT NULL,
  `signal` varchar(255) NOT NULL,
  `metric_value` varchar(64) DEFAULT NULL,
  `action` varchar(255) DEFAULT NULL,
  `status` varchar(32) DEFAULT 'OPEN',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_risk_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardMerchantView.md:merchant_ticket
CREATE TABLE IF NOT EXISTS `merchant_ticket` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `merchant_id` int NOT NULL,
  `platform_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `status` varchar(32) DEFAULT 'OPEN',
  `eta` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_ticket_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardOverview.md:ops_action_item
CREATE TABLE IF NOT EXISTS `ops_action_item` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `owner` varchar(64) NOT NULL,
  `progress` tinyint NOT NULL DEFAULT 0,
  `category` varchar(32) DEFAULT 'CAPACITY',
  `expect_finish_time` datetime DEFAULT NULL,
  `status` varchar(32) DEFAULT 'PENDING',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_action_platform_status` (`platform_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/dashboard/DashboardOverview.md:ops_timeline_event
CREATE TABLE IF NOT EXISTS `ops_timeline_event` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `event_type` varchar(32) NOT NULL COMMENT 'TIMELINE/NOTICE/MAINTENANCE',
  `title` varchar(255) NOT NULL,
  `detail` varchar(512) DEFAULT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `severity` varchar(16) DEFAULT NULL,
  `occur_time` datetime NOT NULL,
  `eta_time` datetime DEFAULT NULL,
  `status` varchar(32) DEFAULT 'OPEN',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_timeline_platform_time` (`platform_id`, `occur_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- md/download/DownloadsHistoryView.md:download_task
CREATE TABLE IF NOT EXISTS `download_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_name` varchar(128) NOT NULL,
  `task_type` varchar(32) NOT NULL COMMENT 'ORDER|BILLING|LEDGER|CUSTOM',
  `status` varchar(16) DEFAULT 'QUEUED' COMMENT 'QUEUED|RUNNING|SUCCESS|FAILED|EXPIRED',
  `file_url` varchar(512) DEFAULT NULL,
  `expires_at` datetime DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `params` json DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导出任务';

-- md/download/DownloadsHistoryView.md:download_task_log
CREATE TABLE IF NOT EXISTS `download_task_log` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `event_time` datetime NOT NULL,
  `status` varchar(16) NOT NULL,
  `message` varchar(255) DEFAULT NULL,
  FOREIGN KEY (`task_id`) REFERENCES `download_task`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导出任务日志';

-- md/finance/FinanceAgentBillingView.md:finance_agent_billing
CREATE TABLE IF NOT EXISTS `finance_agent_billing` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `agent_id` bigint NOT NULL,
  `cycle_start` date NOT NULL,
  `cycle_end` date NOT NULL,
  `settlement_amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_agent_cycle` (`agent_id`,`cycle_start`),
  FOREIGN KEY (`agent_id`) REFERENCES `agent_profile`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理商账单';

-- md/finance/FinanceAgentWithdrawView.md:finance_agent_withdraw
CREATE TABLE IF NOT EXISTS `finance_agent_withdraw` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `agent_id` bigint NOT NULL,
  `amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `bank_account` varchar(128) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'PENDING' COMMENT 'PENDING|APPROVING|PAYING|PAID|FAILED',
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`agent_id`) REFERENCES `agent_profile`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理商提现申请';

-- md/finance/FinanceAgentWithdrawView.md:finance_agent_withdraw_approval
CREATE TABLE IF NOT EXISTS `finance_agent_withdraw_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `withdraw_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`withdraw_id`) REFERENCES `finance_agent_withdraw`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理商提现审批';

-- md/finance/FinanceBillingView.md:finance_billing_approval
CREATE TABLE IF NOT EXISTS `finance_billing_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `billing_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`billing_id`) REFERENCES `finance_billing_cycle`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单审批';

-- md/finance/FinanceBillingView.md:finance_billing_cycle
CREATE TABLE IF NOT EXISTS `finance_billing_cycle` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `subject_type` varchar(16) NOT NULL COMMENT 'MERCHANT|PLATFORM',
  `subject_id` int NOT NULL,
  `cycle_start` date NOT NULL,
  `cycle_end` date NOT NULL,
  `receivable_amount` decimal(18,2) DEFAULT 0,
  `payable_amount` decimal(18,2) DEFAULT 0,
  `status` varchar(16) DEFAULT 'PENDING' COMMENT 'PENDING|REVIEW|PAID|CLOSED',
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_subject_cycle` (`subject_type`,`subject_id`,`cycle_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单周期汇总';

-- md/finance/FinanceSystemTopupView.md:finance_system_topup
CREATE TABLE IF NOT EXISTS `finance_system_topup` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `topup_no` varchar(64) NOT NULL UNIQUE,
  `platform_id` int DEFAULT NULL,
  `channel_id` bigint DEFAULT NULL,
  `amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `status` varchar(16) DEFAULT 'PENDING' COMMENT 'PENDING|APPROVING|SUCCESS|FAILED',
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统付费充值订单';

-- md/finance/FinanceSystemTopupView.md:finance_system_topup_approval
CREATE TABLE IF NOT EXISTS `finance_system_topup_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `topup_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`topup_id`) REFERENCES `finance_system_topup`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统充值审批';

-- md/finance/FinanceWithdrawView.md:finance_withdraw_approval
CREATE TABLE IF NOT EXISTS `finance_withdraw_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `withdraw_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`withdraw_id`) REFERENCES `finance_withdraw_request`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现审批';

-- md/finance/FinanceWithdrawView.md:finance_withdraw_request
CREATE TABLE IF NOT EXISTS `finance_withdraw_request` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `merchant_id` int NOT NULL,
  `channel_id` bigint DEFAULT NULL,
  `amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `status` varchar(16) DEFAULT 'PENDING' COMMENT 'PENDING|APPROVING|PAYING|PAID|FAILED',
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`merchant_id`) REFERENCES `pay_platform_info`(`platform_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现申请';

-- md/merchant/MerchantsBoardView.md:merchant_alert
CREATE TABLE IF NOT EXISTS `merchant_alert` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `merchant_id` int NOT NULL,
  `title` varchar(128) NOT NULL,
  `action` varchar(128) DEFAULT NULL,
  `severity` varchar(16) DEFAULT 'INFO',
  `status` varchar(16) DEFAULT 'OPEN',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`merchant_id`) REFERENCES `pay_platform_info`(`platform_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户提醒/风险';

-- md/merchant/MerchantsBoardView.md:merchant_board_timeline
CREATE TABLE IF NOT EXISTS `merchant_board_timeline` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `merchant_id` int NOT NULL,
  `event_time` datetime NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `event_type` varchar(32) DEFAULT 'INFO',
  FOREIGN KEY (`merchant_id`) REFERENCES `pay_platform_info`(`platform_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户面板时间线';

-- md/merchant/MerchantsBoardView.md:merchant_kpi_snapshot
CREATE TABLE IF NOT EXISTS `merchant_kpi_snapshot` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `merchant_id` int NOT NULL,
  `stat_time` datetime NOT NULL,
  `revenue` decimal(18,2) DEFAULT 0,
  `cost` decimal(18,2) DEFAULT 0,
  `gross_margin_pct` decimal(5,2) DEFAULT 0,
  `risk_score` decimal(5,2) DEFAULT 0,
  `success_rate` decimal(5,2) DEFAULT 0,
  KEY `idx_merchant_time` (`merchant_id`,`stat_time`),
  FOREIGN KEY (`merchant_id`) REFERENCES `pay_platform_info`(`platform_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户 KPI 快照';

-- md/merchant/MerchantsListView.md:merchant_profile
CREATE TABLE IF NOT EXISTS `merchant_profile` (
  `merchant_id` int PRIMARY KEY COMMENT '对应 pay_platform_info.platform_id',
  `merchant_code` varchar(64) NOT NULL,
  `region` varchar(32) DEFAULT NULL,
  `industry` varchar(32) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ENABLED' COMMENT 'ENABLED|FROZEN|TEST',
  `balance` decimal(18,2) DEFAULT 0,
  `currency` varchar(8) DEFAULT 'CNY',
  `risk_level` varchar(16) DEFAULT 'LOW',
  `tags` varchar(128) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`merchant_id`) REFERENCES `pay_platform_info`(`platform_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户运营档案';

-- md/merchant/MerchantsListView.md:merchant_timeline
CREATE TABLE IF NOT EXISTS `merchant_timeline` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `merchant_id` int NOT NULL,
  `event_time` datetime NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `event_type` varchar(32) DEFAULT 'INFO',
  `remark` varchar(255) DEFAULT NULL,
  KEY `idx_merchant_time` (`merchant_id`,`event_time`),
  FOREIGN KEY (`merchant_id`) REFERENCES `pay_platform_info`(`platform_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户操作时间线';

-- md/operate/OpsAppForwarderView.md:forwarder_alert
CREATE TABLE IF NOT EXISTS `forwarder_alert` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `device_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `action` varchar(128) DEFAULT NULL,
  `severity` varchar(16) DEFAULT 'INFO',
  `status` varchar(16) DEFAULT 'OPEN',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`device_id`) REFERENCES `forwarder_device`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信转发告警';

-- md/operate/OpsAppForwarderView.md:forwarder_device
CREATE TABLE IF NOT EXISTS `forwarder_device` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `device_code` varchar(64) NOT NULL UNIQUE,
  `name` varchar(64) NOT NULL,
  `region` varchar(32) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ONLINE' COMMENT 'ONLINE|OFFLINE|MAINTENANCE',
  `forward_rate` decimal(10,2) DEFAULT 0 COMMENT '条/分钟',
  `latency_ms` int DEFAULT 0,
  `ip_address` varchar(64) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `app_version` varchar(32) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信转发设备';

-- md/operate/OpsBatchView.md:ops_batch_approval
CREATE TABLE IF NOT EXISTS `ops_batch_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`task_id`) REFERENCES `ops_batch_task`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营批量审批';

-- md/operate/OpsBatchView.md:ops_batch_task
CREATE TABLE IF NOT EXISTS `ops_batch_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_name` varchar(128) NOT NULL,
  `task_type` varchar(32) NOT NULL COMMENT 'IMPORT|EXPORT|SYNC|OTHER',
  `payload_count` int DEFAULT 0,
  `status` varchar(16) DEFAULT 'QUEUED' COMMENT 'QUEUED|RUNNING|SUCCESS|FAILED',
  `owner_user_id` bigint DEFAULT NULL,
  `file_url` varchar(512) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营批量任务';

-- md/operate/OpsSmsView.md:sms_alert
CREATE TABLE IF NOT EXISTS `sms_alert` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `provider_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `action` varchar(128) DEFAULT NULL,
  `severity` varchar(16) DEFAULT 'INFO',
  `status` varchar(16) DEFAULT 'OPEN',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`provider_id`) REFERENCES `sms_provider`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信告警';

-- md/operate/OpsSmsView.md:sms_metric_snapshot
CREATE TABLE IF NOT EXISTS `sms_metric_snapshot` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `provider_id` bigint NOT NULL,
  `snapshot_time` datetime NOT NULL,
  `send_count` int DEFAULT 0,
  `success_rate` decimal(5,2) DEFAULT 0,
  `latency_ms` int DEFAULT 0,
  `fail_count` int DEFAULT 0,
  KEY `idx_provider_time` (`provider_id`,`snapshot_time`),
  FOREIGN KEY (`provider_id`) REFERENCES `sms_provider`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信指标快照';

-- md/operate/OpsSmsView.md:sms_provider
CREATE TABLE IF NOT EXISTS `sms_provider` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `provider_name` varchar(64) NOT NULL,
  `region` varchar(32) NOT NULL,
  `business_type` varchar(32) DEFAULT 'OTP',
  `status` varchar(16) DEFAULT 'ENABLED' COMMENT 'ENABLED|MAINTENANCE|DISABLED',
  `success_rate` decimal(5,2) DEFAULT 0,
  `latency_ms` int DEFAULT 0,
  `daily_volume` int DEFAULT 0,
  `owner_user_id` bigint DEFAULT NULL,
  `api_key` varchar(128) DEFAULT NULL,
  `api_secret` varchar(128) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信/OTP 供应商';

-- md/operate/OpsTelegramView.md:telegram_alert
CREATE TABLE IF NOT EXISTS `telegram_alert` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `channel_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `action` varchar(128) DEFAULT NULL,
  `severity` varchar(16) DEFAULT 'INFO',
  `status` varchar(16) DEFAULT 'OPEN',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`channel_id`) REFERENCES `telegram_channel`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Telegram 告警';

-- md/operate/OpsTelegramView.md:telegram_channel
CREATE TABLE IF NOT EXISTS `telegram_channel` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `channel_name` varchar(128) NOT NULL,
  `channel_type` varchar(32) NOT NULL COMMENT 'ALERT|BOT|ANNOUNCE',
  `subscribers` int DEFAULT 0,
  `status` varchar(16) DEFAULT 'ENABLED',
  `webhook_url` varchar(256) DEFAULT NULL,
  `bot_token` varchar(128) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Telegram 频道配置';

-- md/operate/OpsVpsManageView.md:ops_vps_alert
CREATE TABLE IF NOT EXISTS `ops_vps_alert` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `vps_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `action` varchar(128) DEFAULT NULL,
  `severity` varchar(16) DEFAULT 'INFO',
  `status` varchar(16) DEFAULT 'OPEN',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`vps_id`) REFERENCES `ops_vps_instance`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VPS 告警';

-- md/operate/OpsVpsManageView.md:ops_vps_instance
CREATE TABLE IF NOT EXISTS `ops_vps_instance` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `vps_code` varchar(64) NOT NULL UNIQUE,
  `name` varchar(128) NOT NULL,
  `region` varchar(32) DEFAULT NULL,
  `ip_address` varchar(64) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ONLINE' COMMENT 'ONLINE|OFFLINE|MAINTENANCE',
  `cpu_usage` decimal(5,2) DEFAULT 0,
  `mem_usage` decimal(5,2) DEFAULT 0,
  `disk_usage` decimal(5,2) DEFAULT 0,
  `owner_user_id` bigint DEFAULT NULL,
  `ssh_user` varchar(64) DEFAULT NULL,
  `ssh_port` int DEFAULT 22,
  `tags` varchar(128) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VPS 实例';

-- md/operate/OpsVpsManageView.md:ops_vps_metric_snapshot
CREATE TABLE IF NOT EXISTS `ops_vps_metric_snapshot` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `vps_id` bigint NOT NULL,
  `snapshot_time` datetime NOT NULL,
  `cpu_usage` decimal(5,2) DEFAULT 0,
  `mem_usage` decimal(5,2) DEFAULT 0,
  `disk_usage` decimal(5,2) DEFAULT 0,
  `network_out` decimal(10,2) DEFAULT 0,
  `network_in` decimal(10,2) DEFAULT 0,
  KEY `idx_vps_time` (`vps_id`,`snapshot_time`),
  FOREIGN KEY (`vps_id`) REFERENCES `ops_vps_instance`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VPS 监控快照';

-- md/order/OrdersApprovalView.md:order_approval_history
CREATE TABLE IF NOT EXISTS `order_approval_history` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `approval_task_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL,
  `status` varchar(16) NOT NULL,
  `operator_user_id` bigint DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `event_time` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`approval_task_id`) REFERENCES `order_approval_task`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批节点历史';

-- md/order/OrdersApprovalView.md:order_approval_task
CREATE TABLE IF NOT EXISTS `order_approval_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `biz_type` varchar(32) NOT NULL COMMENT 'PAYOUT|ROLLBACK|BATCH|MANUAL',
  `biz_id` bigint NOT NULL COMMENT '关联任务或订单 ID',
  `merchant_id` int DEFAULT NULL,
  `amount` decimal(18,2) DEFAULT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `current_stage` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_status_stage` (`status`,`current_stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一审批任务快照';

-- md/order/OrdersBatchView.md:batch_payout_approval
CREATE TABLE IF NOT EXISTS `batch_payout_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`task_id`) REFERENCES `batch_payout_task`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量代付审批';

-- md/order/OrdersBatchView.md:batch_payout_detail
CREATE TABLE IF NOT EXISTS `batch_payout_detail` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `account_name` varchar(128) DEFAULT NULL,
  `account_no` varchar(64) DEFAULT NULL,
  `amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `status` varchar(16) DEFAULT 'PENDING',
  `error_msg` varchar(255) DEFAULT NULL,
  FOREIGN KEY (`task_id`) REFERENCES `batch_payout_task`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量代付明细';

-- md/order/OrdersBatchView.md:batch_payout_task
CREATE TABLE IF NOT EXISTS `batch_payout_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `batch_no` varchar(64) NOT NULL UNIQUE,
  `merchant_id` int NOT NULL,
  `channel_id` bigint NOT NULL,
  `total_amount` decimal(18,2) NOT NULL,
  `total_count` int NOT NULL,
  `status` varchar(16) DEFAULT 'UPLOADED' COMMENT 'UPLOADED|PARSING|APPROVING|PROCESSING|DONE|FAILED',
  `file_url` varchar(512) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量代付任务';

-- md/order/OrdersCollectionView.md:order_collection_timeline
CREATE TABLE IF NOT EXISTS `order_collection_timeline` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `event_time` datetime NOT NULL,
  `event_type` varchar(32) NOT NULL COMMENT 'AUTO_RETRY|MANUAL_FIX|NOTICE',
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  KEY `idx_order_time` (`order_id`,`event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收款订单时间线';

-- md/order/OrdersPayoutQueueView.md:payout_queue_notice
CREATE TABLE IF NOT EXISTS `payout_queue_notice` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `channel_id` bigint DEFAULT NULL,
  `title` varchar(128) NOT NULL,
  `content` varchar(255) DEFAULT NULL,
  `level` varchar(16) DEFAULT 'INFO',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='队列提醒';

-- md/order/OrdersPayoutQueueView.md:payout_queue_snapshot
CREATE TABLE IF NOT EXISTS `payout_queue_snapshot` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `channel_id` bigint NOT NULL,
  `queue_depth` int NOT NULL,
  `dequeue_rate` decimal(10,2) DEFAULT 0 COMMENT '笔/分钟',
  `status` varchar(16) DEFAULT 'NORMAL' COMMENT 'NORMAL|SLOW|BLOCK',
  `retry_rate` decimal(5,2) DEFAULT 0,
  `snapshot_time` datetime NOT NULL,
  `remark` varchar(255) DEFAULT NULL,
  KEY `idx_channel_time` (`channel_id`,`snapshot_time`),
  FOREIGN KEY (`channel_id`) REFERENCES `pay_config_channel`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代付队列快照';

-- md/order/OrdersPayoutView.md:payout_approval_task
CREATE TABLE IF NOT EXISTS `payout_approval_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL COMMENT 'FINANCE|RISK|OPS',
  `status` varchar(16) DEFAULT 'PENDING' COMMENT 'PENDING|APPROVED|REJECTED',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_stage_status` (`stage`,`status`),
  FOREIGN KEY (`order_id`) REFERENCES `order_info`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出款审批任务';

-- md/order/OrdersQueryView.md:order_query_record
CREATE TABLE IF NOT EXISTS `order_query_record` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `merchant_id` int DEFAULT NULL,
  `channel_id` bigint DEFAULT NULL,
  `reason` varchar(255) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING' COMMENT 'PENDING|PROCESSING|DONE|FAILED',
  `owner_user_id` bigint DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`order_id`) REFERENCES `order_info`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查单记录';

-- md/order/OrdersQueryView.md:order_query_timeline
CREATE TABLE IF NOT EXISTS `order_query_timeline` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `query_id` bigint NOT NULL,
  `event_time` datetime NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `event_type` varchar(32) DEFAULT 'INFO',
  `remark` varchar(255) DEFAULT NULL,
  FOREIGN KEY (`query_id`) REFERENCES `order_query_record`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查单时间线';

-- md/order/OrdersRollbackView.md:payout_rollback_approval
CREATE TABLE IF NOT EXISTS `payout_rollback_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`task_id`) REFERENCES `payout_rollback_task`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代付回滚审批';

-- md/order/OrdersRollbackView.md:payout_rollback_task
CREATE TABLE IF NOT EXISTS `payout_rollback_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `channel_id` bigint NOT NULL,
  `mode` varchar(16) NOT NULL COMMENT 'AUTO|MANUAL',
  `amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `status` varchar(16) DEFAULT 'PENDING' COMMENT 'PENDING|APPROVING|PROCESSING|DONE|FAILED',
  `owner_user_id` bigint DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`order_id`) REFERENCES `order_info`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代付回滚任务';

-- md/order/OrdersRollbackView.md:payout_rollback_timeline
CREATE TABLE IF NOT EXISTS `payout_rollback_timeline` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `event_time` datetime NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `event_type` varchar(32) DEFAULT 'INFO',
  FOREIGN KEY (`task_id`) REFERENCES `payout_rollback_task`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代付回滚时间线';

-- md/pay/PaymentsBlacklistView.md:pay_global_blacklist
CREATE TABLE IF NOT EXISTS `pay_global_blacklist` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `target_type` varchar(16) NOT NULL COMMENT 'ENTITY|ACCOUNT|DEVICE',
  `target_value` varchar(128) NOT NULL,
  `reason` varchar(255) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `risk_level` varchar(16) DEFAULT 'MEDIUM',
  `owner` varchar(64) DEFAULT NULL,
  `source` varchar(64) DEFAULT NULL COMMENT '系统/人工/风控模型',
  `extra` json DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_type_value` (`target_type`,`target_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局黑名单';

-- md/pay/PaymentsBlacklistView.md:pay_global_blacklist_action
CREATE TABLE IF NOT EXISTS `pay_global_blacklist_action` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `blacklist_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `progress` tinyint DEFAULT 0,
  `eta` datetime DEFAULT NULL,
  `status` varchar(16) DEFAULT 'OPEN',
  KEY `idx_black_action` (`blacklist_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黑名单联动动作';

-- md/pay/PaymentsBlacklistView.md:pay_global_blacklist_review
CREATE TABLE IF NOT EXISTS `pay_global_blacklist_review` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `blacklist_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL COMMENT 'RISK|OPS|LEGAL',
  `status` varchar(16) DEFAULT 'PENDING',
  `eta` datetime DEFAULT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_black_stage` (`blacklist_id`,`stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黑名单复核';

-- md/pay/PaymentsChannelView.md:pay_channel_maintenance
CREATE TABLE IF NOT EXISTS `pay_channel_maintenance` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `channel_id` bigint NOT NULL,
  `window_start` datetime NOT NULL,
  `window_end` datetime NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'PLANNED',
  `created_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道维护窗口';

-- md/pay/PaymentsChannelView.md:pay_channel_profile
CREATE TABLE IF NOT EXISTS `pay_channel_profile` (
  `channel_id` bigint PRIMARY KEY COMMENT 'pay_config_channel.id',
  `pay_config_id` int NOT NULL,
  `channel_code` varchar(64) NOT NULL,
  `region` varchar(32) DEFAULT NULL,
  `business_type` varchar(32) DEFAULT NULL COMMENT 'UPI/WALLET/BANK/CARD',
  `weight_percent` decimal(5,2) DEFAULT 0,
  `daily_limit` decimal(18,2) DEFAULT NULL,
  `fee_rate` decimal(5,2) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ENABLED',
  `alert_level` varchar(16) DEFAULT 'NORMAL',
  `owner` varchar(64) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道运行配置';

-- md/pay/PaymentsChannelView.md:pay_channel_routing_task
CREATE TABLE IF NOT EXISTS `pay_channel_routing_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `channel_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `progress` tinyint DEFAULT 0,
  `eta` datetime DEFAULT NULL,
  `status` varchar(16) DEFAULT 'OPEN',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_channel_status` (`channel_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道调优项目';

-- md/pay/PaymentsEntityView.md:pay_entity_approval
CREATE TABLE IF NOT EXISTS `pay_entity_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `entity_id` int NOT NULL,
  `title` varchar(128) NOT NULL,
  `stage` varchar(64) NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'OPEN',
  `owner` varchar(64) DEFAULT NULL,
  `apply_user_id` bigint DEFAULT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_entity_stage` (`entity_id`,`status`,`stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主体审批流';

-- md/pay/PaymentsEntityView.md:pay_entity_compliance_metric
CREATE TABLE IF NOT EXISTS `pay_entity_compliance_metric` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `entity_id` int NOT NULL,
  `metric_code` varchar(32) NOT NULL COMMENT 'KYC|CONTRACT|RISK',
  `metric_name` varchar(64) NOT NULL,
  `progress_percent` tinyint NOT NULL DEFAULT 0,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_entity_metric` (`entity_id`,`metric_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主体合规进度';

-- md/pay/PaymentsEntityView.md:pay_entity_document
CREATE TABLE IF NOT EXISTS `pay_entity_document` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `entity_id` int NOT NULL,
  `title` varchar(128) NOT NULL,
  `doc_type` varchar(16) NOT NULL COMMENT 'PDF/DOC/IMG',
  `storage_url` varchar(512) NOT NULL,
  `status` varchar(16) DEFAULT 'VALID',
  `uploaded_by` bigint DEFAULT NULL,
  `uploaded_at` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_entity_doc` (`entity_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主体证照/合同';

-- md/pay/PaymentsEntityView.md:pay_entity_profile
CREATE TABLE IF NOT EXISTS `pay_entity_profile` (
  `entity_id` int PRIMARY KEY COMMENT '对应 pay_platform_info.platform_id',
  `entity_code` varchar(64) NOT NULL COMMENT '对外主体编码，默认沿用 platform_no',
  `region` varchar(32) NOT NULL COMMENT '主要运营区域',
  `industry` varchar(32) DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/FROZEN/IN_REVIEW',
  `risk_level` varchar(16) DEFAULT 'LOW',
  `daily_limit` decimal(18,2) DEFAULT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `owner_user_id` bigint DEFAULT NULL,
  `timezone` varchar(32) DEFAULT 'UTC+05:30',
  `notes` varchar(512) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主体运营档案';

-- md/pay/PaymentsErrorView.md:pay_channel_error_action
CREATE TABLE IF NOT EXISTS `pay_channel_error_action` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `incident_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `progress` tinyint DEFAULT 0,
  `status` varchar(16) DEFAULT 'OPEN',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_incident` (`incident_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错误处理动作';

-- md/pay/PaymentsErrorView.md:pay_channel_error_incident
CREATE TABLE IF NOT EXISTS `pay_channel_error_incident` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `mdc_id` varchar(64) DEFAULT NULL,
  `platform_id` int NOT NULL,
  `pay_config_channel_id` bigint NOT NULL,
  `error_type` varchar(64) NOT NULL,
  `impact_ratio` decimal(5,2) DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'OPEN',
  `severity` varchar(16) DEFAULT 'MEDIUM',
  `owner` varchar(64) DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_channel_status` (`pay_config_channel_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道异常事件';

-- md/pay/PaymentsErrorView.md:pay_channel_error_root_cause
CREATE TABLE IF NOT EXISTS `pay_channel_error_root_cause` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `incident_id` bigint NOT NULL,
  `channel_id` bigint NOT NULL,
  `cause` varchar(255) NOT NULL,
  `resolution` varchar(255) DEFAULT NULL,
  `confirmed_by` varchar(64) DEFAULT NULL,
  `confirmed_at` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_incident_channel` (`incident_id`,`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='根因库';

-- md/pay/PaymentsFallbackView.md:pay_channel_fallback_action
CREATE TABLE IF NOT EXISTS `pay_channel_fallback_action` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `record_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `progress` tinyint DEFAULT 0,
  `eta` datetime DEFAULT NULL,
  KEY `idx_record` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兜底应急动作';

-- md/pay/PaymentsFallbackView.md:pay_channel_fallback_record
CREATE TABLE IF NOT EXISTS `pay_channel_fallback_record` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `strategy_id` bigint NOT NULL,
  `pay_config_channel_id` bigint NOT NULL,
  `trigger_reason` varchar(64) NOT NULL,
  `status` varchar(16) DEFAULT 'ONGOING',
  `owner` varchar(64) DEFAULT NULL,
  `triggered_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `resolved_at` datetime DEFAULT NULL,
  `result` varchar(255) DEFAULT NULL,
  KEY `idx_strategy_status` (`strategy_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兜底执行记录';

-- md/pay/PaymentsFallbackView.md:pay_channel_fallback_strategy
CREATE TABLE IF NOT EXISTS `pay_channel_fallback_strategy` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `pay_config_channel_id` bigint NOT NULL,
  `strategy_name` varchar(128) NOT NULL,
  `mode` varchar(16) NOT NULL COMMENT 'AUTO|MANUAL',
  `status` varchar(16) DEFAULT 'ENABLED',
  `priority` tinyint DEFAULT 1,
  `fallback_channel_id` bigint DEFAULT NULL COMMENT '切换到的备用通道',
  `owner` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道兜底策略';

-- md/pay/PaymentsFallbackView.md:pay_channel_playbook
CREATE TABLE IF NOT EXISTS `pay_channel_playbook` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `title` varchar(128) NOT NULL,
  `status` varchar(16) DEFAULT 'UPDATED',
  `file_url` varchar(512) DEFAULT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应急手册';

-- md/pay/PaymentsIfscBlacklistView.md:ifsc_info
CREATE TABLE IF NOT EXISTS `ifsc_info` (
  `ifsc` varchar(32) PRIMARY KEY,
  `bank_name` varchar(128) DEFAULT NULL,
  `branch` varchar(128) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `city` varchar(64) DEFAULT NULL,
  `district` varchar(64) DEFAULT NULL,
  `state` varchar(64) DEFAULT NULL,
  `phone` varchar(32) DEFAULT NULL,
  `pincode` varchar(16) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IFSC 基础信息';

-- md/pay/PaymentsIfscBlacklistView.md:ifsc_temp
CREATE TABLE IF NOT EXISTS `ifsc_temp` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `ifsc` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `import_batch` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IFSC 批量导入临时表';

-- md/pay/PaymentsIfscBlacklistView.md:upi_blacklist
CREATE TABLE IF NOT EXISTS `upi_blacklist` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `upi` varchar(100) DEFAULT NULL,
  `ifsc` varchar(32) NOT NULL,
  `account_no` varchar(64) DEFAULT NULL,
  `reason` varchar(128) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner` varchar(64) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_ifsc_account` (`ifsc`,`account_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IFSC 公户黑名单';

-- md/pay/PaymentsIfscBlacklistView.md:upi_blacklist_audit
CREATE TABLE IF NOT EXISTS `upi_blacklist_audit` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `black_id` bigint NOT NULL,
  `action` varchar(16) NOT NULL COMMENT 'CREATE|REVIEW|ENABLE|FREEZE',
  `operator` varchar(64) DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_black_action` (`black_id`,`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黑名单审计';

-- md/pay/PaymentsIfscBlacklistView.md:upi_blacklist_log
CREATE TABLE IF NOT EXISTS `upi_blacklist_log` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int DEFAULT NULL,
  `pay_config_id` int DEFAULT NULL,
  `channel_name` varchar(64) DEFAULT NULL,
  `order_id` varchar(64) DEFAULT NULL,
  `status` tinyint DEFAULT 0,
  `result_text` varchar(255) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IFSC 黑名单触发日志';

-- md/pay/PaymentsProfitView.md:pay_channel_fee_config
CREATE TABLE IF NOT EXISTS `pay_channel_fee_config` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `pay_config_channel_id` bigint NOT NULL,
  `fee_rate` decimal(6,4) DEFAULT NULL,
  `fixed_fee` decimal(10,2) DEFAULT 0,
  `currency` varchar(8) DEFAULT 'CNY',
  `effective_date` date NOT NULL,
  `expire_date` date DEFAULT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_channel_effective` (`pay_config_channel_id`,`effective_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道费率配置';

-- md/pay/PaymentsProfitView.md:pay_profit_daily
CREATE TABLE IF NOT EXISTS `pay_profit_daily` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `dimension_type` varchar(16) NOT NULL COMMENT 'ENTITY|CHANNEL|REGION',
  `dimension_id` varchar(64) NOT NULL,
  `revenue_amount` decimal(18,2) DEFAULT 0,
  `cost_amount` decimal(18,2) DEFAULT 0,
  `gross_profit` decimal(18,2) DEFAULT 0,
  `gross_margin` decimal(6,3) DEFAULT 0,
  `txn_volume` decimal(18,2) DEFAULT 0,
  `trend` decimal(6,3) DEFAULT 0,
  `currency` varchar(8) DEFAULT 'CNY',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_dimension_day` (`stat_date`,`dimension_type`,`dimension_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='利润日统计';

-- md/pay/PaymentsProfitView.md:pay_profit_notice
CREATE TABLE IF NOT EXISTS `pay_profit_notice` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `title` varchar(128) NOT NULL,
  `content` varchar(512) DEFAULT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='利润公告';

-- md/pay/PaymentsReconcileView.md:pay_reconcile_action
CREATE TABLE IF NOT EXISTS `pay_reconcile_action` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `file_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `progress` tinyint DEFAULT 0,
  `eta` datetime DEFAULT NULL,
  `status` varchar(16) DEFAULT 'OPEN',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_file_status` (`file_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账处理行动';

-- md/pay/PaymentsReconcileView.md:pay_reconcile_file
CREATE TABLE IF NOT EXISTS `pay_reconcile_file` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `pay_config_channel_id` bigint NOT NULL,
  `file_date` date NOT NULL,
  `currency` varchar(8) NOT NULL,
  `diff_count` int DEFAULT 0,
  `diff_amount` decimal(18,2) DEFAULT 0,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner` varchar(64) DEFAULT NULL,
  `file_url` varchar(512) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_channel_date` (`pay_config_channel_id`,`file_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道对账文件';

-- md/pay/PaymentsReconcileView.md:pay_reconcile_timeline
CREATE TABLE IF NOT EXISTS `pay_reconcile_timeline` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `file_id` bigint DEFAULT NULL,
  `event_time` datetime NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `event_type` varchar(16) DEFAULT 'SYSTEM',
  KEY `idx_file_time` (`file_id`,`event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账时间线';

-- md/pay/PaymentsRollbackView.md:pay_channel_rollback_approval
CREATE TABLE IF NOT EXISTS `pay_channel_rollback_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `stage` varchar(64) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `comment` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_task_stage` (`task_id`,`stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回滚审批实例';

-- md/pay/PaymentsRollbackView.md:pay_channel_rollback_step
CREATE TABLE IF NOT EXISTS `pay_channel_rollback_step` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `step_code` varchar(32) NOT NULL COMMENT 'APPROVAL|SYNC|ALERT',
  `label` varchar(64) NOT NULL,
  `value` varchar(64) DEFAULT NULL,
  `detail` varchar(255) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'OPEN',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_task_step` (`task_id`,`step_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回滚关键步骤进度';

-- md/pay/PaymentsRollbackView.md:pay_channel_rollback_task
CREATE TABLE IF NOT EXISTS `pay_channel_rollback_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `pay_config_channel_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `mode` varchar(16) NOT NULL COMMENT 'AUTO|MANUAL',
  `amount` decimal(18,2) DEFAULT 0,
  `currency` varchar(8) DEFAULT 'CNY',
  `status` varchar(16) NOT NULL DEFAULT 'PENDING',
  `owner` varchar(64) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_channel_status` (`pay_config_channel_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道回滚任务';

-- md/pay/PaymentsThrottleView.md:pay_channel_limit_event
CREATE TABLE IF NOT EXISTS `pay_channel_limit_event` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `pay_config_channel_id` bigint NOT NULL,
  `trigger_reason` varchar(64) NOT NULL,
  `limit_desc` varchar(64) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ONGOING',
  `ownership` varchar(32) DEFAULT NULL COMMENT 'riskbot/ops',
  `triggered_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `released_at` datetime DEFAULT NULL,
  KEY `idx_channel_status` (`pay_config_channel_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限流事件';

-- md/pay/PaymentsThrottleView.md:pay_channel_limit_plan
CREATE TABLE IF NOT EXISTS `pay_channel_limit_plan` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `progress` tinyint DEFAULT 0,
  `eta` datetime DEFAULT NULL,
  `status` varchar(16) DEFAULT 'OPEN',
  KEY `idx_event` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扩容/解限计划';

-- md/pay/PaymentsThrottleView.md:pay_config_limit
CREATE TABLE IF NOT EXISTS `pay_config_limit` (
  `pay_config_id` int PRIMARY KEY,
  `limit_amount` decimal(18,2) NOT NULL,
  `total_amount` decimal(18,2) DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0=禁用,1=限流, -1=预警',
  `begin_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `end_time` datetime DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道限额配置';

-- md/pay/PaymentsThrottleView.md:pay_config_limit_record
CREATE TABLE IF NOT EXISTS `pay_config_limit_record` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `pay_config_id` int NOT NULL,
  `limit_amount` decimal(18,2) NOT NULL,
  `total_amount` decimal(18,2) DEFAULT 0,
  `status` tinyint NOT NULL,
  `begin_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `end_time` datetime DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  KEY `idx_pay_config_id` (`pay_config_id`,`begin_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限流操作日志';

-- md/system/SystemBillingView.md:system_billing_approval
CREATE TABLE IF NOT EXISTS `system_billing_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `billing_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`billing_id`) REFERENCES `system_billing_order`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统账单审批';

-- md/system/SystemBillingView.md:system_billing_detail
CREATE TABLE IF NOT EXISTS `system_billing_detail` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `billing_id` bigint NOT NULL,
  `item_name` varchar(128) NOT NULL,
  `item_amount` decimal(18,2) NOT NULL,
  `vendor` varchar(128) DEFAULT NULL,
  `note` varchar(255) DEFAULT NULL,
  FOREIGN KEY (`billing_id`) REFERENCES `system_billing_order`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单明细';

-- md/system/SystemBillingView.md:system_billing_order
CREATE TABLE IF NOT EXISTS `system_billing_order` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `billing_no` varchar(64) NOT NULL UNIQUE,
  `project` varchar(64) NOT NULL COMMENT '费用类型，如 CLOUD|MONITOR|LICENSE',
  `cycle_start` date NOT NULL,
  `cycle_end` date NOT NULL,
  `amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `status` varchar(16) DEFAULT 'PENDING' COMMENT 'PENDING|APPROVING|PAID|CLOSED',
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统内部账单';

-- md/system/SystemSettingsView.md:system_feature_flag
CREATE TABLE IF NOT EXISTS `system_feature_flag` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `flag_key` varchar(128) NOT NULL UNIQUE,
  `description` varchar(255) DEFAULT NULL,
  `enabled` tinyint(1) DEFAULT 0,
  `scope` varchar(128) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灰度开关';

-- md/system/SystemSettingsView.md:system_setting
CREATE TABLE IF NOT EXISTS `system_setting` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `setting_key` varchar(128) NOT NULL UNIQUE,
  `setting_name` varchar(128) NOT NULL,
  `value` text NOT NULL,
  `value_type` varchar(16) DEFAULT 'STRING',
  `category` varchar(64) DEFAULT 'GENERAL',
  `description` varchar(255) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE|DISABLED|GRAY',
  `gray_scope` varchar(128) DEFAULT NULL COMMENT '灰度命中规则，如平台ID列表',
  `owner_user_id` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';

-- md/system/SystemSettingsView.md:system_setting_audit
CREATE TABLE IF NOT EXISTS `system_setting_audit` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `setting_id` bigint NOT NULL,
  `version` int NOT NULL,
  `value_snapshot` text NOT NULL,
  `change_type` varchar(32) NOT NULL COMMENT 'CREATE|UPDATE|DELETE|GRAY',
  `operator_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`setting_id`) REFERENCES `system_setting`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置审计';

-- md/system/SystemUsersView.md:system_role
CREATE TABLE IF NOT EXISTS `system_role` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `role_code` varchar(64) NOT NULL UNIQUE,
  `role_name` varchar(64) NOT NULL,
  `level` varchar(32) DEFAULT NULL,
  `permissions` json DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色';

-- md/system/SystemUsersView.md:system_user
CREATE TABLE IF NOT EXISTS `system_user` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account` varchar(64) NOT NULL UNIQUE,
  `name` varchar(64) NOT NULL,
  `email` varchar(128) DEFAULT NULL,
  `mobile` varchar(32) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE|FROZEN|DISABLED|RISK|REVIEW',
  `risk_level` varchar(16) DEFAULT 'LOW',
  `last_login_at` datetime DEFAULT NULL,
  `last_login_ip` varchar(64) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL COMMENT '负责人/直属上级',
  `tags` varchar(128) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台系统用户';

-- md/system/SystemUsersView.md:system_user_audit
CREATE TABLE IF NOT EXISTS `system_user_audit` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `event_time` datetime NOT NULL,
  `event` varchar(128) NOT NULL,
  `detail` varchar(255) DEFAULT NULL,
  `operator_id` bigint DEFAULT NULL,
  FOREIGN KEY (`user_id`) REFERENCES `system_user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户操作审计';

-- md/system/SystemUsersView.md:system_user_role
CREATE TABLE IF NOT EXISTS `system_user_role` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  FOREIGN KEY (`user_id`) REFERENCES `system_user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`role_id`) REFERENCES `system_role`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联';

