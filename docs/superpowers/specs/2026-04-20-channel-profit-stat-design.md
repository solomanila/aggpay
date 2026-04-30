# 通道利润日统计功能设计

**日期**: 2026-04-20  
**状态**: 已批准

---

## 背景

利润报表页面（收款通道 Tab）需要按通道、按日期展示成交金额和利润相关字段。系统需要每天凌晨自动统计前一天各通道的实际成交额（`system_amount`）以及通道费收入（`channel_fee_income`），并将结果持久化到 admin schema 统计表，供前端查询。

---

## 约束

- admin-service 只能直接访问 `admin` schema，禁止直连 `order_info`（属于 pay-service）
- 跨服务数据通过 Feign 调用获取，方向为 admin → pay（单向）
- 本期只做收款通道（`channel_type` 暂不区分），不实现导入/编辑功能

---

## 统计表设计（admin schema）

```sql
CREATE TABLE `channel_profit_stat` (
  `id`                   BIGINT        NOT NULL AUTO_INCREMENT,
  `stat_date`            DATE          NOT NULL  COMMENT '统计日期',
  `channel_id`           BIGINT        NOT NULL  COMMENT 'pay_config_channel.id',
  `channel_name`         VARCHAR(128)  NOT NULL  COMMENT '通道名称',
  `system_amount`        DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '系统计算金额：当日 status=1 的 real_amount 之和',
  `channel_fee_income`   DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '通道费收入：Σ(商户金额 × merchant_channel_config.fee_rate)',
  `collection_amount`    DECIMAL(18,4) DEFAULT NULL COMMENT '代收户金额（人工录入）',
  `dropped_order_income` DECIMAL(18,4) DEFAULT NULL COMMENT '掉单收入（人工录入）',
  `channel_cost`         DECIMAL(18,4) DEFAULT NULL COMMENT '通道成本（人工录入）',
  `other_cost`           DECIMAL(18,4) DEFAULT NULL COMMENT '其他成本（人工录入）',
  `frozen_amount`        DECIMAL(18,4) DEFAULT NULL COMMENT '冻结金额（人工录入）',
  `adjustment`           DECIMAL(18,4) DEFAULT NULL COMMENT '调差（人工录入）',
  `profit`               DECIMAL(18,4) DEFAULT NULL COMMENT '利润（人工录入）',
  `create_time`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_channel` (`stat_date`, `channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道利润日统计';
```

---

## 数据流

```
[admin-service Scheduler]  每天 01:00 AM
  ↓ 计算昨日日期 (LocalDate.now().minusDays(1))
  ↓ 调用 PayServiceClient.getDailyChannelPlatformStats(date)
  
[pay-service]
  ExtChannelProfitStatMapper.queryDailyStatsByPlatform(statDate)
  SELECT c.id, c.title, o.platform_id, SUM(o.real_amount)
  FROM pay_config_channel c
  LEFT JOIN order_info o ON o.pay_config_channel_id = c.id
    AND o.status = 1 AND DATE(o.create_time) = #{statDate}
  GROUP BY c.id, c.title, o.platform_id
  → 返回 List<ChannelPlatformStatDTO>

[admin-service]
  按 channel_id 分组
  对每个 (channelId, platformId) 查 merchant_channel_config.fee_rate
  channel_fee_income = Σ(platformAmount × feeRate)
  system_amount      = Σ(platformAmount) [全部商户汇总]
  UPSERT channel_profit_stat (ON DUPLICATE KEY UPDATE)
```

---

## 新增 DTO（common-core）

```java
// ChannelPlatformStatDTO
String     statDate;     // yyyy-MM-dd
Long       channelId;
String     channelName;
Integer    platformId;   // 商户平台ID，null 表示该通道当日无订单
BigDecimal systemAmount; // 该商户在该通道当日成交额
```

---

## API

### pay-service 新增

```
GET /api/pay/dashboard/dailyChannelPlatformStats?date=yyyy-MM-dd
→ R<List<ChannelPlatformStatDTO>>
```

### admin-service CRUD

```
GET /admin/channel-profit/list
    ?channelName=   (可选，模糊)
    &startDate=     (可选，yyyy-MM-dd)
    &endDate=       (可选，yyyy-MM-dd)
    &pageNum=1
    &pageSize=20
→ R<Page<ChannelProfitStat>>
```

---

## 涉及文件清单

| 模块 | 文件 | 操作 |
|------|------|------|
| `common-core` | `ChannelPlatformStatDTO.java` | 新增 |
| `pay-service` | `ExtChannelProfitStatMapper` | 新增方法 `queryDailyStatsByPlatform` |
| `pay-service` | pay-service Controller（dashboard 相关） | 新增接口方法 |
| `admin-service` | `PayServiceClient` | 新增 Feign 方法 |
| `admin-service` | `PayServiceFacade` | 新增 facade 方法 |
| `admin-service` | `ChannelProfitStat.java` (Entity) | 新增 |
| `admin-service` | `ChannelProfitStatMapper.java` | 新增 |
| `admin-service` | `ChannelProfitStatService.java` | 新增 |
| `admin-service` | `ChannelProfitStatController.java` | 新增 |
| `admin-service` | `ChannelProfitStatScheduler.java` | 新增 |
| `backend/docs` | `channel_profit_stat.sql` | 新增 |
