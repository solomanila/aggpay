# 微服务跨 Schema 查询禁止规则

## 背景

在重构商户面板之前，admin-service 直接通过 MyBatis-Plus Mapper 使用 `@TableName("aggpay.*")`
跨 schema 查询 aggpay 数据库的表。这违反了微服务数据库隔离原则。

**发现时间**: 2026-04-19  
**修复方式**: 全部替换为 Feign 调用 pay-service 暴露的接口

---

## 已修复的跨 Schema 操作

| 原文件 | 原 @TableName | 问题 | 修复方式 |
|--------|--------------|------|---------|
| `admin-service/.../entity/PayPlatformInfo.java` | `aggpay.pay_platform_info` | admin-service 直接读写 aggpay 商户表 | 删除实体；改为 Feign 调用 `/dashboard/merchantProfiles`、`/dashboard/merchantPage` 等 |
| `admin-service/.../entity/PayConfigChannel.java` | `aggpay.pay_config_channel` | admin-service 直接读取 aggpay 通道表 | 删除实体；改为 Feign 调用 `/dashboard/channelsByIds` |
| `admin-service/.../entity/AggPayConfigInfo.java` | `aggpay.pay_config_info` | admin-service 直接读取 aggpay 支付配置表 | 删除实体；通道 title 由 pay-service 在 `getChannelsByIds` 中内聚查询 |
| `admin-service/.../mapper/PayPlatformInfoMapper.java` | — | 跨 schema Mapper | 删除 |
| `admin-service/.../mapper/PayConfigChannelMapper.java` | — | 跨 schema Mapper | 删除 |
| `admin-service/.../mapper/AggPayConfigInfoMapper.java` | — | 跨 schema Mapper | 删除 |

---

## 受影响的服务

### admin-service（调用方）

**MerchantBoardService** — 商户面板矩阵数据
- 商户列表: `PayServiceFacade.fetchMerchantProfiles()` → Feign `GET /api/pay/dashboard/merchantProfiles`
- 通道列表: `PayServiceFacade.fetchChannelsByIds(ids)` → Feign `GET /api/pay/dashboard/channelsByIds`
- 通道选项: `PayServiceFacade.fetchChannelConfigList()` → 已有 Feign（复用）

**MerchantManageService** — 商户 CRUD
- 分页查询: `PayServiceFacade.fetchMerchantPage()` → Feign `GET /api/pay/dashboard/merchantPage`
- 详情查询: `PayServiceFacade.fetchMerchantDetail()` → Feign `GET /api/pay/dashboard/merchantDetail/{id}`
- 创建: `PayServiceFacade.createMerchant()` → Feign `POST /api/pay/dashboard/merchantCreate`
- 更新: `PayServiceFacade.updateMerchant()` → Feign `PUT /api/pay/dashboard/merchantUpdate/{id}`
- 切换状态: `PayServiceFacade.toggleMerchantStatus()` → Feign `PATCH /api/pay/dashboard/merchantStatus/{id}`
- 删除: `PayServiceFacade.deleteMerchant()` → Feign `DELETE /api/pay/dashboard/merchant/{id}`
- 重置密钥: `PayServiceFacade.resetMerchantKey()` → Feign `PATCH /api/pay/dashboard/merchantResetKey/{id}`

### pay-service（数据所有方）

新增接口（均在 `DashboardMetricsController` 和 `DashboardMetricsService`）：
- `GET /api/pay/dashboard/merchantProfiles` — 所有启用商户列表
- `GET /api/pay/dashboard/merchantPage` — 分页查询商户
- `GET /api/pay/dashboard/merchantDetail/{platformId}` — 商户详情
- `POST /api/pay/dashboard/merchantCreate` — 创建商户（含 platformNo/secretKey 生成）
- `PUT /api/pay/dashboard/merchantUpdate/{platformId}` — 更新商户
- `PATCH /api/pay/dashboard/merchantStatus/{platformId}` — 切换状态
- `DELETE /api/pay/dashboard/merchant/{platformId}` — 删除商户
- `PATCH /api/pay/dashboard/merchantResetKey/{platformId}` — 重置密钥
- `GET /api/pay/dashboard/channelsByIds` — 按 ID 列表获取通道信息

### common-core（共享 DTO）

新增：
- `MerchantProfileDTO` — platformId, platformNo, title, secretKey, nullify, createTime
- `BoardChannelDTO` — id (Long), title, payConfigId, payConfigTitle

---

## 禁止规则

### admin-service 中严禁

1. 在任何实体类中使用 `@TableName("aggpay.*")` 形式的跨 schema 表名
2. 注入任何以 `aggpay.*` 为目标表的 Mapper
3. 直接执行任何针对 `aggpay` schema 中表的 SQL 或 MyBatis-Plus 查询

### 正确做法

- `aggpay.*` 表的所有读写均须通过 `PayServiceClient` (Feign) → `PayServiceFacade` 封装后调用
- 如需访问新的 `aggpay.*` 数据，在 pay-service 新增接口，在 admin-service 通过上述路径调用

### Schema 归属

| Schema | 归属服务 | 访问方式 |
|--------|---------|---------|
| `admin` | admin-service | 直接 MyBatis-Plus Mapper |
| `aggpay` | pay-service | Feign → PayServiceClient → PayServiceFacade |
