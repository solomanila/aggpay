-- Sample seed data for admin-service consolidated schema

INSERT INTO system_user_auth (id, account, name, email, mobile, status, risk_level, owner_user_id, tags, last_login_at, last_login_ip, password_hash, password_salt, password_algo, password_updated_at, force_reset, google_secret, google_enabled, google_last_verified_at, created_at)
VALUES
  (1001, 'ops.liang', '梁运营', 'ops.liang@example.com', '+8613812345678', 'ACTIVE', 'LOW', NULL, JSON_ARRAY('OPS','DASHBOARD'), NOW(), '10.0.0.5', '$2a$10$OpxBbb7aexamplehash000001', NULL, 'bcrypt', NOW(), 0, 'JBSWY3DPEHPK3PXP', 1, NOW(), NOW()),
  (1002, 'risk.wei', '魏风控', 'risk.wei@example.com', '+8618712345678', 'ACTIVE', 'MEDIUM', 1001, JSON_ARRAY('RISK'), NOW(), '10.0.0.6', '$2a$10$OpxBbb7aexamplehash000002', NULL, 'bcrypt', NOW(), 0, 'NB2W45DFOIZA====', 1, NOW(), NOW()),
  (1003, 'finance.chen', '陈财务', 'finance.chen@example.com', '+862112345678', 'ACTIVE', 'LOW', 1001, JSON_ARRAY('FINANCE'), NOW(), '10.0.0.7', '$2a$10$OpxBbb7aexamplehash000003', NULL, 'bcrypt', NOW(), 1, NULL, 0, NULL, NOW());

INSERT INTO system_setting (id, setting_key, setting_name, value, value_type, category, description, status, gray_scope, owner_user_id)
VALUES
  (1, 'dashboard.cache.ttl', '仪表盘缓存秒数', '60', 'NUMBER', 'DASHBOARD', '首页缓存 TTL', 'ACTIVE', NULL, 1001),
  (2, 'payout.queue.alert', '代付队列告警阈值', '{"pending":500,"latency":30000}', 'JSON', 'OPERATIONS', '触发通知的队列阈值', 'ACTIVE', 'platform:in', 1001);

INSERT INTO system_setting_audit (setting_id, version, value_snapshot, change_type, operator_user_id)
VALUES
  (1, 1, '60', 'CREATE', 1001),
  (2, 1, '{"pending":500,"latency":30000}', 'CREATE', 1002);

INSERT INTO system_feature_flag (id, flag_key, description, enabled, scope, owner_user_id)
VALUES
  (1, 'dashboard.newHero', '启用新首页 hero 模块', 1, 'ops-team', 1001);

INSERT INTO merchant_profile (id, merchant_code, platform_id, name, region, business_type, tier, status, risk_level, owner_user_id, tags, contact_name, contact_info)
VALUES
  (2001, 'MER-IND-PIX', 101, 'IndusPay PIX', 'IN', 'PIX', 'A', 'ACTIVE', 'LOW', 1001, JSON_ARRAY('PIX','Realtime'), 'Rahul', '+91-8800-1234'),
  (2002, 'MER-BR-PIX', 102, 'Brasil Turbo', 'BR', 'PIX', 'B', 'ACTIVE', 'MEDIUM', 1001, JSON_ARRAY('PIX','VIP'), 'Luisa', '+55-11-9988-1234'),
  (2003, 'MER-SEA-WALLET', 103, 'SEA WalletHub', 'SG', 'WALLET', 'A', 'ACTIVE', 'LOW', 1002, JSON_ARRAY('Wallet','Priority'), 'Wei Ming', '+65-8888-6666');

INSERT INTO merchant_metric_snapshot (merchant_id, stat_time, gmv, success_rate, refund_rate, retention7, retention14, retention30, risk_score, cost, gross_margin_pct)
VALUES
  (2001, DATE_SUB(NOW(), INTERVAL 1 DAY), 320000.50, 97.80, 0.40, 45.0, 33.0, 21.0, 12.5, 210000.00, 34.40),
  (2002, DATE_SUB(NOW(), INTERVAL 1 DAY), 510000.00, 96.10, 1.20, 40.0, 28.0, 18.0, 24.0, 360000.00, 29.50),
  (2003, DATE_SUB(NOW(), INTERVAL 1 DAY), 180000.75, 98.60, 0.20, 52.0, 41.0, 25.0, 10.0, 120000.00, 33.30);

INSERT INTO merchant_engagement (merchant_id, type, title, content, owner, severity, status, metric_value, action, eta)
VALUES
  (2001, 'TICKET', 'PIX 通道常驻巡检', '巡检延后 2 小时', 'ops.liang', 'INFO', 'IN_PROGRESS', NULL, '协调通道商', '今日 18:00'),
  (2002, 'ALERT', '退款率飙升', '今日退款率 1.2%，高于阈值', 'risk.wei', 'HIGH', 'OPEN', 'refund=1.2%', '与风控同步', '今日 20:00'),
  (2003, 'CAMPAIGN', '618 钱包活动', '投入 5 w 预算，预计提升 12%', 'ops.liang', 'INFO', 'ONGOING', 'lift=+12%', '继续监控 GMV', '6/18');

INSERT INTO merchant_timeline (merchant_id, event_time, event_type, title, detail, operator_user_id)
VALUES
  (2001, NOW(), 'INFO', 'PIX SLA 达标', '过去 24h 成功率 98%+', 1001),
  (2002, NOW(), 'RISK', '退款提醒', '退款率持续两小时高于 1%', 1002);

INSERT INTO agent_profile (id, agent_code, name, region, status, quota_total, quota_used, balance, owner_user_id, contact, tags)
VALUES
  (3001, 'AG-BLR', 'Bangalore Agent Hub', 'IN', 'ENABLED', 500000.00, 220000.00, 180000.00, 1001, 'agent.blr@example.com', JSON_ARRAY('UPI','PIX')),
  (3002, 'AG-SP', 'São Paulo Switch', 'BR', 'MAINTENANCE', 600000.00, 400000.00, 90000.00, 1002, 'agent.sp@example.com', JSON_ARRAY('PIX'));

INSERT INTO pay_entity_profile (id, entity_code, name, country, status, license_no, compliance_score, documents, owner_user_id)
VALUES
  (5001, 'ENT-IN-01', 'Indus Payments Pvt', 'IN', 'ACTIVE', 'IN-PSP-9988', 92.5, JSON_ARRAY('license.pdf','aml.pdf'), 1001),
  (5002, 'ENT-BR-01', 'BrasilSwitch LTDA', 'BR', 'ACTIVE', 'BR-CEN-1122', 88.0, JSON_ARRAY('license.pdf'), 1001);

INSERT INTO pay_channel_profile (id, channel_code, name, area_type, status, entity_id, business_types, fee_rate, cost_rate, routing_weight, limit_config, owner_user_id, tags)
VALUES
  (5101, 'CHN-PIX-A', 'PIX 极速 A', 'BR', 'ACTIVE', 5002, JSON_ARRAY('PIX','PAYIN'), 0.0180, 0.0120, 60, JSON_OBJECT('tps', 3500), 1001, JSON_ARRAY('VIP','LowLatency')),
  (5102, 'CHN-UPI-STACK', 'UPI 智选栈', 'IN', 'ACTIVE', 5001, JSON_ARRAY('UPI','PAYIN','PAYOUT'), 0.0120, 0.0090, 80, JSON_OBJECT('tps', 5000), 1001, JSON_ARRAY('Fallback'));

INSERT INTO agent_channel_binding (agent_id, channel_id, quota_percent, status)
VALUES
  (3001, 5102, 40.0, 'ENABLED'),
  (3002, 5101, 55.0, 'MAINTENANCE');

INSERT INTO bank_supplier (id, supplier_code, name, country, status, owner_user_id, contact, tags)
VALUES
  (6001, 'BKS-IN-01', 'ICICI Corporate', 'IN', 'ACTIVE', 1003, 'supplier.icici@example.com', JSON_ARRAY('Tier1')),
  (6002, 'BKS-BR-02', 'Banco do Brasil', 'BR', 'ACTIVE', 1003, 'supplier.bb@example.com', JSON_ARRAY('PIX'));

INSERT INTO bank_account (id, supplier_id, account_code, bank_name, branch, account_no, currency, status, balance, daily_limit, limit_usage_pct, risk_level, platform_scope, owner_user_id, tags)
VALUES
  (6101, 6001, 'ACC-IN-UPI-001', 'ICICI', 'Bangalore MG', '1122334455', 'INR', 'ENABLED', 850000.00, 1200000.00, 70.2, 'LOW', 'IN', 1003, JSON_ARRAY('UPI','Priority')),
  (6102, 6002, 'ACC-BR-PIX-001', 'Banco do Brasil', 'São Paulo HQ', '7788990011', 'BRL', 'ENABLED', 430000.00, 900000.00, 48.5, 'MEDIUM', 'BR', 1003, JSON_ARRAY('PIX'));

INSERT INTO bank_account_metric_snapshot (account_id, stat_time, inflow_amount, outflow_amount, success_rate, queue_depth, alert_level)
VALUES
  (6101, NOW(), 280000.00, 255000.00, 98.5, 23, 'NORMAL'),
  (6102, NOW(), 330000.00, 310000.00, 96.1, 41, 'WARN');

INSERT INTO bank_account_event (account_id, event_time, event_type, amount, balance_after, description, operator_user_id, status)
VALUES
  (6101, NOW(), 'LEDGER', 12000.00, 862000.00, 'UPI 批量入账', 1003, 'POSTED'),
  (6102, NOW(), 'ALERT', NULL, 430000.00, 'PIX 队列超过 40', 1002, 'OPEN');

INSERT INTO bank_bookkeeping_entry (account_id, entry_type, amount, currency, direction, status, owner_user_id, approval_stage, approval_status, approval_comment, evidences)
VALUES
  (6101, 'ADJUST', 5000.00, 'INR', 'DEBIT', 'APPROVED', 1003, 'FINANCE', 'APPROVED', '调账已确认', JSON_ARRAY('voucher-123.pdf')),
  (6102, 'BOOK', 8000.00, 'BRL', 'CREDIT', 'PENDING', 1003, 'FINANCE', 'PENDING', NULL, NULL);

INSERT INTO pay_channel_operation (channel_id, event_time, event_type, severity, status, metric_value, description, action_plan, owner_user_id)
VALUES
  (5101, NOW(), 'ERROR', 'HIGH', 'OPEN', 'error_rate=3%', 'PIX 回调失败率上升', '切换到备用节点', 1001),
  (5102, NOW(), 'FALLBACK', 'INFO', 'RESOLVED', 'traffic=-15%', 'UPI 自动降容触发', '恢复常规流量', 1001);

INSERT INTO pay_channel_limit_plan (channel_id, plan_type, limit_value, window_minutes, threshold, status, effective_from, effective_to)
VALUES
  (5101, 'EMERGENCY', 250000.00, 10, 200000.00, 'ACTIVE', NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY)),
  (5102, 'STATIC', 500000.00, 60, 480000.00, 'ACTIVE', NOW(), NULL);

INSERT INTO pay_reconcile_file (channel_id, file_date, file_name, status, diff_count, action, notes)
VALUES
  (5101, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'pix_2026-04-01.csv', 'PROCESSING', 3, '人工复核', JSON_ARRAY('diff-order-889','diff-order-900')),
  (5102, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'upi_2026-04-01.csv', 'DONE', 0, '自动完成', NULL);

INSERT INTO payout_batch (batch_no, type, status, total_amount, total_count, success_count, failed_count, payload, approval_flow, owner_user_id, remark)
VALUES
  ('PB20260402001', 'PAYMENT', 'EXECUTING', 980000.00, 420, 400, 20, JSON_ARRAY('ORD123','ORD124'), JSON_OBJECT('stage','FINANCE','owner','finance.chen'), 1003, '印度 UPI 大批次'),
  ('PB20260402002', 'ROLLBACK', 'PENDING', 450000.00, 120, 0, 0, JSON_ARRAY('ORD889','ORD890'), JSON_OBJECT('stage','OPS','owner','ops.liang'), 1001, '巴西 PIX 补单');

INSERT INTO payout_queue_event (queue_code, event_time, event_type, pending_count, throughput, latency_ms, content)
VALUES
  ('UPI-PAYOUT', NOW(), 'SNAPSHOT', 180, 520, 18000, JSON_OBJECT('warn','false')),
  ('PIX-PAYOUT', NOW(), 'ALERT', 620, 310, 42000, JSON_OBJECT('warn','true','reason','backlog'));

INSERT INTO payout_rollback_task (order_id, status, reason, timeline, owner_user_id)
VALUES
  (900112233, 'IN_REVIEW', '客户申诉退款', JSON_ARRAY('申请-客服','复核-OPS'), 1002);

INSERT INTO order_operation_timeline (order_id, order_type, event_time, event_type, content, operator_user_id)
VALUES
  (900112233, 'PAYOUT', NOW(), 'QUERY', JSON_OBJECT('channel','PIX','result','处理中'), 1001),
  (900009999, 'COLLECTION', NOW(), 'DISPUTE', JSON_OBJECT('merchant','MER-BR-PIX','reason','未到账'), 1002);

INSERT INTO finance_billing (billing_no, billing_type, cycle_start, cycle_end, amount, currency, status, items, approval_flow, owner_user_id, remark)
VALUES
  ('FB-202603', 'CLOUD', '2026-03-01', '2026-03-31', 56000.00, 'CNY', 'APPROVING', JSON_ARRAY('aws-mumbai','aliyun-singapore'), JSON_OBJECT('stage','FINANCE','next','CFO'), 1003, '三月云资源'),
  ('FB-202604', 'THIRD_PARTY', '2026-04-01', '2026-04-30', 120000.00, 'USD', 'PENDING', JSON_ARRAY('pix-provider','upi-license'), JSON_OBJECT('stage','OPS','next','FINANCE'), 1001, '供应商费用');

INSERT INTO finance_settlement (settle_type, entity_id, amount, currency, status, approval_flow, evidence, owner_user_id)
VALUES
  ('WITHDRAW', 2002, 320000.00, 'BRL', 'APPROVED', JSON_OBJECT('stage','FINANCE','approvedBy','finance.chen'), JSON_ARRAY('wire-proof.pdf'), 1003),
  ('SYSTEM_TOPUP', NULL, 500000.00, 'USD', 'PENDING', JSON_OBJECT('stage','OPS'), NULL, 1001);

INSERT INTO download_task (task_code, task_name, status, params, file_path, expire_at, owner_user_id, audit_log)
VALUES
  ('DL-PIX-APR', 'PIX 对账文件', 'READY', JSON_OBJECT('channel','PIX','date','2026-04-01'), '/downloads/pix_20260401.zip', DATE_ADD(NOW(), INTERVAL 7 DAY), 1001, JSON_ARRAY('创建','生成')), 
  ('DL-AGENT-MAR', '代理提现历史', 'GENERATING', JSON_OBJECT('agent','AG-BLR','month','2026-03'), NULL, NULL, 1002, JSON_ARRAY('创建'));

INSERT INTO ops_batch_task (task_code, task_name, task_type, status, payload, owner_user_id, approval_flow, result_summary)
VALUES
  ('BATCH-TELE-001', 'Telegram 批量推送', 'TELEGRAM', 'COMPLETED', JSON_OBJECT('targets',5), 1001, JSON_OBJECT('approvedBy','ops.liang'), JSON_OBJECT('success',5)),
  ('BATCH-UPI-002', 'UPI 通道调权', 'PAY_CHANNEL', 'PENDING', JSON_OBJECT('channels', JSON_ARRAY('UPI-A','UPI-B')), 1001, JSON_OBJECT('stage','RISK'), NULL);

INSERT INTO ops_notification (channel, target, title, content, level, status, meta, created_at, sent_at)
VALUES
  ('TELEGRAM', '@opswarroom', 'PIX backlog 告警', '待处理 620 单', 'CRITICAL', 'SENT', JSON_OBJECT('queue','PIX-PAYOUT'), NOW(), NOW()),
  ('SMS', '+919900112233', 'UPI 降容通知', '已降容 15%', 'INFO', 'PENDING', JSON_OBJECT('channel','UPI'), NOW(), NULL);

INSERT INTO fund_snapshot (stat_time, platform_id, balance_in, balance_out, in_transit, pipeline)
VALUES
  (NOW(), 101, 1200000.00, 980000.00, 220000.00, JSON_OBJECT('upi', 450000, 'pix', 530000)),
  (NOW(), 102, 880000.00, 760000.00, 140000.00, JSON_OBJECT('pix', 620000));
