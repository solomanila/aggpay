/*
 Navicat Premium Data Transfer

 Source Server         : 127.0.0.1
 Source Server Type    : MySQL
 Source Server Version : 80041 (8.0.41)
 Source Host           : 127.0.0.1:3306
 Source Schema         : pay

 Target Server Type    : MySQL
 Target Server Version : 80041 (8.0.41)
 File Encoding         : 65001

 Date: 21/03/2026 20:39:56
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for merchant_info
-- ----------------------------
DROP TABLE IF EXISTS `merchant_info`;
CREATE TABLE `merchant_info` (
  `pay_config_id` int NOT NULL COMMENT '关联的支付配置ID (主键部分, 通常外键指向 pay_config_info.id)',
  `platform_id` int NOT NULL COMMENT '关联的平台ID (主键部分, 通常外键指向 platform_info.id)',
  `app_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用ID (App ID)',
  `private_key` text COLLATE utf8mb4_unicode_ci COMMENT '商户主私钥 (通常用于签名和API认证)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `private_key1` text COLLATE utf8mb4_unicode_ci COMMENT '商户备用私钥1或特定用途私钥',
  `private_key2` text COLLATE utf8mb4_unicode_ci COMMENT '商户备用私钥2或特定用途私钥',
  `private_key3` text COLLATE utf8mb4_unicode_ci COMMENT '商户备用私钥3或特定用途私钥',
  `private_key4` text COLLATE utf8mb4_unicode_ci COMMENT '商户备用私钥4或特定用途私钥',
  PRIMARY KEY (`pay_config_id`,`platform_id`),
  KEY `idx_mi_app_id` (`app_id`),
  KEY `idx_mi_create_time` (`create_time`),
  KEY `fk_merchant_info_platform` (`platform_id`),
  CONSTRAINT `fk_merchant_info_pay_config` FOREIGN KEY (`pay_config_id`) REFERENCES `pay_config_info` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_merchant_info_platform` FOREIGN KEY (`platform_id`) REFERENCES `pay_platform_info` (`platform_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户（针对特定支付配置和平台）的密钥及应用信息表';

-- ----------------------------
-- Table structure for order_build_error
-- ----------------------------
DROP TABLE IF EXISTS `order_build_error`;
CREATE TABLE `order_build_error` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `mdc_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MDC跟踪ID (用于日志链路追踪)',
  `platform_id` int DEFAULT NULL COMMENT '平台ID',
  `pay_config_id` int DEFAULT NULL COMMENT '支付配置ID',
  `pay_config_channel_id` bigint DEFAULT NULL COMMENT '支付配置渠道ID',
  `channel_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '渠道名称',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '错误发生时间',
  `class_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发生错误的类名',
  `error_text` text COLLATE utf8mb4_unicode_ci COMMENT '错误详情/堆栈信息',
  `result_text` text COLLATE utf8mb4_unicode_ci COMMENT '相关的结果或上下文文本',
  PRIMARY KEY (`id`),
  KEY `idx_mdc_id` (`mdc_id`),
  KEY `idx_platform_id` (`platform_id`),
  KEY `idx_pay_config_channel_id` (`pay_config_channel_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_class_name` (`class_name`(191))
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单构建/处理错误记录表';

-- ----------------------------
-- Table structure for order_callback
-- ----------------------------
DROP TABLE IF EXISTS `order_callback`;
CREATE TABLE `order_callback` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '回调类型 (例如: PAYMENT_SUCCESS, REFUND_NOTICE)',
  `platform_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台订单号或商户订单号',
  `pay_config_id` int DEFAULT NULL COMMENT '支付配置ID',
  `req_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '回调请求的完整URL',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '回调记录创建时间',
  `create_ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发起回调的IP地址',
  `status` int DEFAULT NULL COMMENT '回调处理状态 (例如: 0-待处理, 1-成功, 2-失败)',
  `param` text COLLATE utf8mb4_unicode_ci COMMENT '回调接收到的参数内容 (例如: JSON, XML或form-data字符串)',
  PRIMARY KEY (`id`),
  KEY `idx_platform_no` (`platform_no`),
  KEY `idx_type` (`type`),
  KEY `idx_pay_config_id` (`pay_config_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单回调记录表';

-- ----------------------------
-- Table structure for order_info
-- ----------------------------
DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订单号',
  `platform_id` int DEFAULT NULL COMMENT '平台ID',
  `front_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '前端订单号/商户订单号',
  `pay_config_id` int DEFAULT NULL COMMENT '支付配置ID',
  `user_id` int DEFAULT NULL COMMENT '用户ID',
  `game_id` int DEFAULT NULL COMMENT '游戏ID',
  `status` int DEFAULT NULL COMMENT '订单状态',
  `req_amount` decimal(18,2) DEFAULT NULL COMMENT '请求金额',
  `real_amount` decimal(18,2) DEFAULT NULL COMMENT '实际支付金额',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_status` int DEFAULT NULL COMMENT '创建状态',
  `create_ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建IP',
  `notice_status` int DEFAULT NULL COMMENT '通知状态',
  `notice_time` datetime DEFAULT NULL COMMENT '通知时间',
  `pay_config_channel_id` bigint DEFAULT NULL COMMENT '支付配置渠道ID',
  `other_order_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '其他订单号 (例如上游渠道订单号)',
  `on_line_id` bigint DEFAULT NULL COMMENT '在线ID (具体含义需根据业务确定)',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `extend1` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '扩展字段1',
  `extend2` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '扩展字段2',
  `extend3` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '扩展字段3',
  `sync_status` int DEFAULT NULL COMMENT '同步状态',
  `settle_amount` decimal(18,2) DEFAULT NULL COMMENT '结算金额',
  `upi` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'UPI (统一支付接口) 相关信息',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_front_id` (`front_id`),
  KEY `idx_other_order_id` (`other_order_id`),
  KEY `idx_status` (`status`),
  KEY `idx_pay_time` (`pay_time`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单信息表';

-- ----------------------------
-- Table structure for order_notify_record
-- ----------------------------
DROP TABLE IF EXISTS `order_notify_record`;
CREATE TABLE `order_notify_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_id` int DEFAULT NULL COMMENT '平台ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '通知记录创建时间',
  `create_ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '接收通知的请求来源IP',
  `pay_config_id` int DEFAULT NULL COMMENT '支付配置ID',
  `sign_check` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '签名校验结果或相关信息',
  `params_map` text COLLATE utf8mb4_unicode_ci COMMENT '接收到的通知参数Map序列化字符串 (例如: JSON)',
  `pay_config_parameters` text COLLATE utf8mb4_unicode_ci COMMENT '支付配置相关参数序列化字符串 (例如: JSON)',
  `log_text` text COLLATE utf8mb4_unicode_ci COMMENT '相关日志文本',
  `class_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理通知的类名',
  PRIMARY KEY (`id`),
  KEY `idx_platform_id` (`platform_id`),
  KEY `idx_pay_config_id` (`pay_config_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_class_name` (`class_name`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单通知记录表';

-- ----------------------------
-- Table structure for order_req_record
-- ----------------------------
DROP TABLE IF EXISTS `order_req_record`;
CREATE TABLE `order_req_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_id` int DEFAULT NULL COMMENT '平台ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `pay_config_id` int DEFAULT NULL COMMENT '支付配置ID',
  `pay_config_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '支付配置名称',
  `pay_config_channel_id` bigint DEFAULT NULL COMMENT '支付配置渠道ID',
  `pay_config_channel_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '支付配置渠道名称',
  `req_link` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求链接',
  `req_time` bigint DEFAULT NULL COMMENT '请求耗时(毫秒)或请求时间戳',
  `body_text` text COLLATE utf8mb4_unicode_ci COMMENT '请求体内容',
  `result_text` text COLLATE utf8mb4_unicode_ci COMMENT '响应结果内容',
  `error_text` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息内容',
  `log_text` text COLLATE utf8mb4_unicode_ci COMMENT '日志文本内容',
  `error` int DEFAULT NULL COMMENT '错误标识 (例如: 0表示无错误, 1或错误码表示有错误)',
  PRIMARY KEY (`id`),
  KEY `idx_platform_id` (`platform_id`),
  KEY `idx_pay_config_id` (`pay_config_id`),
  KEY `idx_pay_config_channel_id` (`pay_config_channel_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_error` (`error`)
) ENGINE=InnoDB AUTO_INCREMENT=77 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单请求记录表';

-- ----------------------------
-- Table structure for order_virtual_account
-- ----------------------------
DROP TABLE IF EXISTS `order_virtual_account`;
CREATE TABLE `order_virtual_account` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` int DEFAULT NULL COMMENT '用户ID',
  `platform_id` int DEFAULT NULL COMMENT '平台ID',
  `game_id` int DEFAULT NULL COMMENT '游戏ID',
  `pay_config_id` int DEFAULT NULL COMMENT '支付配置ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '账户持有人姓名',
  `primary_contact` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主要联系方式 (例如手机号或邮箱)',
  `contact_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系方式类型 (例如: MOBILE, EMAIL)',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱地址',
  `mobile` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号码',
  `virtual_accounts_id` bigint DEFAULT NULL COMMENT '虚拟账户系统中的ID (若对接外部系统)',
  `virtual_account_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '虚拟银行账号',
  `virtual_account_ifsc_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '虚拟账户IFSC代码 (例如印度支付)',
  `vpa` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '虚拟支付地址 (例如UPI VPA)',
  `landline_number` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '座机号码',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  `extend1` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '扩展字段1',
  `extend2` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '扩展字段2',
  `account_num` int DEFAULT NULL COMMENT '账户数量或编号 (具体业务含义待定)',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_platform_id` (`platform_id`),
  KEY `idx_virtual_account_number` (`virtual_account_number`),
  KEY `idx_vpa` (`vpa`),
  KEY `idx_email` (`email`),
  KEY `idx_mobile` (`mobile`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单相关的虚拟账户信息表';

-- ----------------------------
-- Table structure for pay_config_channel
-- ----------------------------
DROP TABLE IF EXISTS `pay_config_channel`;
CREATE TABLE `pay_config_channel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pay_config_id` int DEFAULT NULL COMMENT '关联的支付配置ID (外键指向 pay_config_info.id)',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '渠道标题/名称',
  `json_param` text COLLATE utf8mb4_unicode_ci COMMENT '渠道特定的JSON格式配置参数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `share_id` int DEFAULT NULL COMMENT '共享ID或权重ID (具体业务含义待定)',
  `extract_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '提取类型 (例如: JSONPath, REGEX, XPath)',
  `extract_field` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '提取字段的路径或表达式 (例如: $.data.token)',
  `extract_prefix` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '提取值的前缀',
  `status` int DEFAULT '0' COMMENT ' 0  1 ',
  PRIMARY KEY (`id`),
  KEY `idx_pay_config_id` (`pay_config_id`),
  KEY `idx_title` (`title`),
  KEY `idx_share_id` (`share_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付配置的渠道信息表';

-- ----------------------------
-- Table structure for pay_config_info
-- ----------------------------
DROP TABLE IF EXISTS `pay_config_info`;
CREATE TABLE `pay_config_info` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `short_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '短代码/唯一标识',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置标题/名称',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注信息',
  `url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主要请求URL (例如支付网关地址)',
  `third_service` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '第三方服务名称/标识',
  `call_method` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '调用方法 (例如 HTTP方法 GET/POST, 或API方法名)',
  `req_param` text COLLATE utf8mb4_unicode_ci COMMENT '请求参数模板或默认参数 (例如 JSON格式)',
  `nullify` tinyint(1) DEFAULT '0' COMMENT '是否作废/禁用 (0-启用, 1-作废/禁用)',
  `area_type` int DEFAULT NULL COMMENT '区域类型/适用地区代码',
  `test` tinyint(1) DEFAULT '0' COMMENT '是否测试配置 (0-正式, 1-测试)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `http_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'HTTP类型 (例如 HTTP, HTTPS, 或Content-Type)',
  `we_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '特定用途URL (例如 微信相关URL, 回调URL等)',
  `req_domain` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求域名 (用于动态构建URL或校验)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_short_code` (`short_code`),
  KEY `idx_title` (`title`),
  KEY `idx_third_service` (`third_service`),
  KEY `idx_nullify` (`nullify`),
  KEY `idx_test` (`test`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付配置信息表';

-- ----------------------------
-- Table structure for pay_config_ip
-- ----------------------------
DROP TABLE IF EXISTS `pay_config_ip`;
CREATE TABLE `pay_config_ip` (
  `pay_config_id` int NOT NULL COMMENT '支付配置ID',
  `callback_ip` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '回调IP',
  `nullify` int DEFAULT '0' COMMENT '是否作废',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `back_count` bigint DEFAULT '0' COMMENT '回调次数',
  PRIMARY KEY (`pay_config_id`,`callback_ip`),
  UNIQUE KEY `uniq_pay_config_ip` (`pay_config_id`,`callback_ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付回调IP表';

-- ----------------------------
-- Table structure for pay_config_notify
-- ----------------------------
DROP TABLE IF EXISTS `pay_config_notify`;
CREATE TABLE `pay_config_notify` (
  `pay_config_id` int NOT NULL COMMENT '关联的支付配置ID (外键, 主键部分)',
  `type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知类型 (例如: PAYMENT_SUCCESS, REFUND, 主键部分)',
  `order_param` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知中表示“我方订单号”的参数名',
  `success_param` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知中表示“成功状态”的参数名',
  `success_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知中表示“成功”的参数值',
  `success_result` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '成功处理通知后，需返回给第三方的内容 (例如: success, OK)',
  `amount_param` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知中表示“金额”的参数名',
  `amount_unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '金额单位 (例如: CENT 表示分, YUAN 表示元)',
  `other_order_id_param` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知中表示“第三方或渠道订单号”的参数名',
  `settle_amount_param` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知中表示“结算金额”的参数名',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '此通知配置的创建时间',
  `fail_param` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知中表示“失败状态”的参数名',
  `fail_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知中表示“失败”的参数值或错误码',
  `refund_param` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知中表示“退款状态或金额”的参数名',
  `refund_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知中表示“退款特定状态”的参数值',
  `sign_check` tinyint(1) DEFAULT '1' COMMENT '是否需要验签 (0-否, 1-是)',
  `ip_check` tinyint(1) DEFAULT '1' COMMENT '是否需要IP校验 (0-否, 1-是)',
  `amount_check` tinyint(1) DEFAULT '1' COMMENT '是否需要金额校验 (0-否, 1-是)',
  `platform_check` tinyint(1) DEFAULT '1' COMMENT '是否需要平台特定校验 (0-否, 1-是)',
  `upi_param` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知中表示UPI的参数名',
  `ifsc_param` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知中表示IFSC代码的参数名',
  PRIMARY KEY (`pay_config_id`,`type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付配置的通知解析规则表';

-- ----------------------------
-- Table structure for pay_config_parameter
-- ----------------------------
DROP TABLE IF EXISTS `pay_config_parameter`;
CREATE TABLE `pay_config_parameter` (
  `pay_config_id` int NOT NULL COMMENT '关联的支付配置ID (主键部分, 外键指向 pay_config_info.id)',
  `pay_index` int NOT NULL COMMENT '参数在此配置中的顺序或索引号 (主键部分)',
  `pay_config_channel_id` bigint DEFAULT NULL COMMENT '关联的支付渠道ID (可选, 外键指向 pay_config_channel.id)',
  `pay_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '参数的业务名称/标识符 (例如: merchant_id, api_key, secret_key)',
  `pay_value` text COLLATE utf8mb4_unicode_ci COMMENT '参数的实际值 (可能需要加密存储)',
  `type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '参数的数据类型或用途分类 (例如: STRING, TEXT, INTEGER, BOOLEAN, FILE_PATH, SECRET)',
  `config_enum` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '参数的可选枚举值或相关配置说明 (例如JSON数组或特定格式字符串)',
  PRIMARY KEY (`pay_config_id`,`pay_index`),
  KEY `idx_pcp_pay_config_channel_id` (`pay_config_channel_id`),
  KEY `idx_pcp_pay_name` (`pay_name`),
  CONSTRAINT `fk_pcp_pay_config_channel_id` FOREIGN KEY (`pay_config_channel_id`) REFERENCES `pay_config_channel` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_pcp_pay_config_id` FOREIGN KEY (`pay_config_id`) REFERENCES `pay_config_info` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付配置的参数详情表';

-- ----------------------------
-- Table structure for pay_ip_white
-- ----------------------------
DROP TABLE IF EXISTS `pay_ip_white`;
CREATE TABLE `pay_ip_white` (
  `platform_id` int NOT NULL COMMENT '关联的平台ID (主键部分, 通常外键指向 platform_info.id)',
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'IP地址 (IPv4 或 IPv6, 主键部分)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '白名单记录创建时间',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态 (0-无效/禁用, 1-有效/启用)',
  PRIMARY KEY (`platform_id`,`ip_address`),
  KEY `idx_piw_status` (`status`),
  KEY `idx_piw_create_time` (`create_time`),
  CONSTRAINT `fk_piw_platform_id` FOREIGN KEY (`platform_id`) REFERENCES `pay_platform_info` (`platform_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付IP白名单表';

-- ----------------------------
-- Table structure for pay_platform_info
-- ----------------------------
DROP TABLE IF EXISTS `pay_platform_info`;
CREATE TABLE `pay_platform_info` (
  `platform_id` int NOT NULL AUTO_INCREMENT COMMENT '平台唯一ID (主键)',
  `platform_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台编号/代码 (业务唯一标识)',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台名称/标题',
  `domain` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台主域名',
  `secret_key` text COLLATE utf8mb4_unicode_ci COMMENT '平台密钥 (用于签名或API认证, 请注意加密存储)',
  `nullify` tinyint(1) DEFAULT '0' COMMENT '是否作废/禁用 (0-启用, 1-作废/禁用)',
  `area_type` int DEFAULT NULL COMMENT '区域类型/适用地区代码',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `callback_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台默认回调URL (接收支付结果通知等)',
  `we_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '特定用途URL (例如微信相关或其他)',
  PRIMARY KEY (`platform_id`),
  UNIQUE KEY `uk_platform_no` (`platform_no`),
  KEY `idx_ppi_title` (`title`),
  KEY `idx_ppi_domain` (`domain`),
  KEY `idx_ppi_nullify` (`nullify`),
  KEY `idx_ppi_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付平台信息表';

SET FOREIGN_KEY_CHECKS = 1;
