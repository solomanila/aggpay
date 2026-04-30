-- ============================================================
-- 测试数据（对应截图中的商户）
-- platform_id 对应 aggpay.pay_platform_info.platform_id
-- 假设截图中商户 platform_id 分别为: 1001(INA061), 1002(test), 1003(luckbey)
--   Reelbear-Artopay=1004, Luckyseal-Spay=1005
-- ============================================================

-- ------------------------------------------------------------
-- merchant_op_config
-- ------------------------------------------------------------
INSERT INTO `merchant_op_config`
  (platform_id, email, agent_id, remark,
   daily_pay_order_limit, daily_withdraw_count_limit, daily_withdraw_amount_limit,
   daily_pay_limit, daily_payout_limit,
   large_payout_risk_enabled, large_payout_risk_amount,
   telegram_group_id, settlement_notify)
VALUES
  -- INA061（截图 Image13 数据）
  (1001, 'ina061@example.com', NULL, NULL,
   5, 5, 50000.00,
   NULL, NULL,
   1, 20000.00,
   '-5173486896', 1),

  -- test
  (1002, 'test@example.com', NULL, NULL,
   5, 5, 50000.00,
   NULL, NULL,
   0, NULL,
   NULL, 1),

  -- luckbey
  (1003, NULL, NULL, NULL,
   5, 5, 50000.00,
   NULL, NULL,
   0, NULL,
   NULL, 1),

  -- Reelbear-Artopay（截图 Image2 数据）
  (1004, NULL, NULL, NULL,
   5, 5, 50000.00,
   NULL, NULL,
   1, 500000.00,
   '-5244190744', 1),

  -- Luckyseal-Spay
  (1005, NULL, NULL, NULL,
   5, 5, 50000.00,
   NULL, NULL,
   0, NULL,
   NULL, 1);


-- ------------------------------------------------------------
-- merchant_balance  （截图余额列：INR:240.30, RUB:4175.0 等）
-- ------------------------------------------------------------
INSERT INTO `merchant_balance` (platform_id, currency, available, frozen)
VALUES
  (1001, 'INR',  0.0000,    0.0000),
  (1002, 'INR',  240.3000,  0.0000),  -- 截图 21SVIP INR:240.30
  (1003, 'RUB',  0.0000,    0.0000),
  (1004, 'RUB',  0.0000,    0.0000),
  (1005, 'RUB',  0.0000,    0.0000),
  -- wolfjJYY-Sbp RUB:4175.0（截图）
  (1006, 'RUB',  4175.0000, 0.0000);


-- ------------------------------------------------------------
-- merchant_balance_log  （充值示例流水）
-- ------------------------------------------------------------
INSERT INTO `merchant_balance_log`
  (platform_id, currency, op_type, amount,
   before_available, after_available, before_frozen, after_frozen,
   remark, operator_id)
VALUES
  (1002, 'INR', 'RECHARGE', 240.3000,
   0.0000, 240.3000, 0.0000, 0.0000,
   '初始充值', 1),

  (1006, 'RUB', 'RECHARGE', 4175.0000,
   0.0000, 4175.0000, 0.0000, 0.0000,
   '初始充值', 1),

  (1005, 'RUB', 'FREEZE', 100.0000,
   500.0000, 400.0000, 0.0000, 100.0000,
   '订单冻结', 1),

  (1005, 'RUB', 'UNFREEZE', 100.0000,
   400.0000, 500.0000, 100.0000, 0.0000,
   '订单解冻', 1);


-- ------------------------------------------------------------
-- merchant_channel_config
-- 通道 ID 对应 aggpay.pay_config_channel:
--   air-in=101, air-in(M352693)-DP=102, air-in(M352893)-DP=103
--   air-in(M352898)-DP=104, air-in(M352899)-DP=105, air-in(M352900)-DP=106
--   Indohealth-out=201, INR-AIM-OUT=202, ltpay-out=203, nodel-out=204
-- ------------------------------------------------------------
INSERT INTO `merchant_channel_config`
  (platform_id, pay_config_channel_id, channel_type,
   daily_limit, weight, enabled,
   start_time, end_time, settlement_cycle, auto_settle,
   min_amount, max_amount,
   currency, pay_mode, pay_page,
   fee_rate, fee_fixed, tiered_rate_enabled)
VALUES
  -- INA061 代收通道（截图面板绿色激活）
  (1001, 102, 'PAYMENT', 5000000.00,  1, 1, NULL, NULL, 30, 1, 300, 5000,  'INR', 'WEB', 'INR-native page (Include GPAY)', 8.0000, 0.0000, 0),
  (1001, 103, 'PAYMENT', 50000000.00, 1, 1, NULL, NULL, 30, 1, 300, 5000,  'INR', 'WEB', 'INR-native page (Include GPAY)', 8.0000, 0.0000, 0),
  (1001, 104, 'PAYMENT', 5000000.00,  1, 1, NULL, NULL, 30, 1, 300, 5000,  'INR', 'WEB', NULL,                               8.0000, 0.0000, 0),
  (1001, 105, 'PAYMENT', 5000000.00,  1, 1, NULL, NULL, 30, 1, 300, 5000,  'INR', 'WEB', NULL,                               8.0000, 0.0000, 0),
  (1001, 106, 'PAYMENT', 5000000.00,  1, 1, NULL, NULL, 30, 1, 300, 5000,  'INR', 'WEB', NULL,                               8.0000, 0.0000, 0),

  -- INA061 代付通道
  (1001, 201, 'PAYOUT',  0.00, 1, 1, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, 0.0000, 0.0000, 0),

  -- test 代收（截图面板 100K 日限额）
  (1002, 101, 'PAYMENT', 100000.00, 1, 1, NULL, NULL, NULL, 0, 0, NULL, 'INR', NULL, NULL, 0.0000, 0.0000, 0),
  (1002, 102, 'PAYMENT', 100000.00, 1, 1, NULL, NULL, NULL, 0, 0, NULL, 'INR', NULL, NULL, 0.0000, 0.0000, 0),
  (1002, 103, 'PAYMENT', 100000.00, 1, 1, NULL, NULL, NULL, 0, 0, NULL, 'INR', NULL, NULL, 0.0000, 0.0000, 0),
  (1002, 104, 'PAYMENT', 100000.00, 1, 1, NULL, NULL, NULL, 0, 0, NULL, 'INR', NULL, NULL, 0.0000, 0.0000, 0),
  (1002, 105, 'PAYMENT', 100000.00, 1, 1, NULL, NULL, NULL, 0, 0, NULL, 'INR', NULL, NULL, 0.0000, 0.0000, 0),
  (1002, 106, 'PAYMENT',  50000.00, 1, 1, NULL, NULL, NULL, 0, 0, NULL, 'INR', NULL, NULL, 0.0000, 0.0000, 0),

  -- test 代付
  (1002, 203, 'PAYOUT',  0.00, 1, 1, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, 0.0000, 0.0000, 0),

  -- luckbey 代付
  (1003, 202, 'PAYOUT',  0.00, 1, 1, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, 0.0000, 0.0000, 0);
