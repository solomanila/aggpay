SHOPLINE合作伙伴应用入驻：


1、开发者中心账户注册：https://admin.myshopline.com/user/signUp?sl_state=scene=developer

2、开放平台入驻流程：https://developer.myshopline.com/docsv2/ec20/3cv5d7wpfgr6a8z5/3msx146abt1t6k7d?version=v20230901

3、API接口文档：https://developer.shopline.com/docsv2/ec20/3cv5d7wpfgr6a8z5/3pjerhtbfsebya9m?version=v20230901

4、支付应用开发文档：https://developer.myshopline.com/docsv2/ec20/3cv5d7wpfgr6a8z5/kjqd1k7ueatarjk3?version=v20230901

5、授权机制：https://developer.myshopline.com/docsv2/ec20/3cv5d7wpfgr6a8z5/teva9rwr8948egze?version=v20230901



根据文档，给你列出完整的对接计划：

---

## 当前进度
✅ 第一步：注册开发者账号
✅ 第二步：创建公共应用
🔄 **当前位置：等待获取支付扩展权限**

---

## 详细对接计划

### 第一阶段：权限申请（当前）
**任务：**
- 拿到刚创建应用的 **AppKey**
- 联系 SHOPLINE 生态团队，告知 AppKey，申请开通支付扩展权限
- 等待平台返回**公钥**

**注意：** 同时需要准备好正式环境和测试环境两个 URL 地址（你的支付后端服务地址）

---

### 第二阶段：技术对接开发

拿到权限和公钥后开始开发，按以下顺序阅读文档：

| 顺序 | 文档 | 目的 |
|------|------|------|
| 1 | 授权机制文档 | 理解 OAuth 授权流程 |
| 2 | App Bridge（必须对接）| 前端嵌入框架 |
| 3 | 支付应用文档 | 支付核心接口 |
| 4 | 商家安装支付应用流程 | 理解商家侧流程 |

**需要开发的核心模块：**

```
1. OAuth授权回调接口
   商家安装应用时触发，获取店铺访问令牌

2. 支付下单接口
   接收SHOPLINE订单，调用你的支付通道，返回支付URL

3. 支付回调接口（Webhook）
   接收支付结果通知，更新订单状态

4. 退款接口
   处理商家发起的退款请求

5. App Bridge前端页面
   内嵌在SHOPLINE后台的支付配置页面
```

---

### 第三阶段：应用配置

**第四步 - 完善应用基础设置：**
- 填写应用名称、描述
- 填入正式环境和测试环境 URL
- 保存基础设置

**第五步 - 完善支付应用扩展：**
- 点击「支付应用扩展」→「打开草稿」
- 填写以下信息：

| 字段 | 内容 |
|------|------|
| 商家管理后台URL | 你的内嵌配置页面地址 |
| 提款名称 | 你的支付方式名称（如：XXX Pay）|
| 收款方式 | 选择对应收款类型 |
| 可用国家/地区 | 选择印度（IN）|
| API版本 | 填写当前版本 |
| 商家展示方式 | 支付提供商渠道 |
| 公钥 | 填入平台返回的公钥 |

- 点击「创建版本」→「提交审核」

---

### 第四阶段：审核上线

| 步骤 | 操作 | 预计耗时 |
|------|------|---------|
| 第六步 | 通过支付扩展审核后点击「发布」| 审核1-3天 |
| 第七步 | 完善应用详情和多语言配置，提交SL应用审核 | 审核1-3天 |
| 第八步 | 提供测试店铺handle，自行测试支付流程走通 | 1-2天 |
| 第九步 | 通知SHOPLINE团队验收 | 验收1-3天 |
| 第十步 | 进入灰度阶段，小范围商家使用 | 观察期 |
| 正式上线 | 交易量达标后全量上线 | 视流水情况 |

---

### 第五阶段：测试checklist

上线前必须自测通过以下流程：

```
✅ 商家安装应用 → OAuth授权成功
✅ 商家后台配置支付参数
✅ 买家下单 → 跳转支付页面
✅ 支付成功 → 订单状态更新
✅ 支付失败 → 订单状态更新
✅ 商家发起退款 → 退款成功
✅ 退款失败场景处理
```

---

## 当前最紧急的事

**现在马上要做的两件事：**

1. 把 AppKey 发给 SHOPLINE 生态团队申请支付权限
2. 把你的支付后端服务部署好，准备好正式和测试两个可访问的 HTTPS 地址

权限下来之前可以先把授权机制和 App Bridge 文档读透，不浪费等待时间。

---

## 对接过程中踩过的坑

### 坑一：签名验证失败（key 不匹配）

**现象：** 调用 `/api/pay/shopline/pay` 返回 `签名验证失败`。

**原因：** `ShoplinePaymentController.main()` 里硬编码了一套测试密钥对用于本地生成签名，但 `application-test.yml` 中 `shopline.public-key` 配置的是另一套公钥，两者不一致导致验签失败。

```
application.yml（默认）  → public-key: MIIBIjAN...zv0w/...
application-test.yml    → public-key: MIIBIjAN...2zkSKtgm8...  ← 两者不同
```

**规则：** `main()` 方法中的密钥对必须与当前环境 `shopline.public-key` 配置一致；切换环境时两处同步更新。

---

### 坑二：SSL 证书不受信任（Let's Encrypt 换了新根证书）

**现象：** Shopline Java 客户端报 `javax.net.ssl.SSLPeerUnverifiedException`。

**原因：** Let's Encrypt 已将新证书迁移到 **ISRG Root YR** 根证书体系（中间 CA 为 YR1/YR2），该根证书尚未被大多数 Java 版本的 cacerts 收录。`--preferred-chain "ISRG Root X1"` 在完全切换后已无效。

**解决方案：** 改用 **ZeroSSL**（根证书为 Sectigo，所有 Java 版本均信任）。

```bash
# 需先在 zerossl.com 注册账号，在 Developer 页面生成 EAB 凭证
certbot certonly --webroot -w /var/www/<webroot> \
  --server https://acme.zerossl.com/v2/DV90 \
  --email <email> \
  --agree-tos \
  --eab-kid <EAB_KID> \
  --eab-hmac-key <EAB_HMAC_KEY> \
  -d <domain>

systemctl reload nginx
```

验证根证书：
```bash
openssl x509 -noout -issuer -in /etc/letsencrypt/live/<domain>/chain.pem
# 期望：issuer=... Sectigo ...
```

**附：排查 SSL 链路的正确姿势**
```bash
# 检查服务端实际发送了几张证书（应为 2~3 张）
echo | openssl s_client -connect <domain>:443 -showcerts 2>/dev/null | grep -c "BEGIN CERTIFICATE"

# 检查中间证书链到哪个根
openssl x509 -noout -subject -issuer -in /etc/letsencrypt/live/<domain>/chain.pem
```

> 注意：`openssl s_client ... | grep -E "(subject|issuer)="` 只显示叶子证书信息，不能判断链路完整性，有误导性。

**Nginx 续期后必须重载，建议配置自动 hook：**
```bash
echo -e '#!/bin/bash\nsystemctl reload nginx' > /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh
chmod +x /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh
```

---

### 坑三：Hostname not verified（回调 URL 用了 IP）

**现象：** Shopline 回调时报 `Hostname 192.131.142.181 not verified`，证书 SAN 只有域名。

**原因：** 下单时传给 Shopline 的 `notifyUrl` 用了服务器 IP，SSL 证书不覆盖 IP，回调验证失败。

**规则：** 所有传给 Shopline 的回调地址必须使用 HTTPS 域名：
```
notifyUrl / redirectUrl / cancelUrl 必须是 https://<domain>/...，不得用 IP
```

同时注意 `ShoplineConfig.loginBaseUrl` 默认值写死了 IP，部署时必须在配置文件中显式覆盖：
```yaml
shopline:
  login-base-url: https://test.zyapay.com   # test 环境
  # login-base-url: https://pay.zyapay.com  # prod 环境
```

---

### 切换域名 test.zyapay.com → pay.zyapay.com 检查清单

| 项目 | 说明 |
|---|---|
| SSL 证书 | 用 ZeroSSL 为 `pay.zyapay.com` 申请新证书（EAB 凭证可复用同一 ZeroSSL 账号） |
| Nginx 配置 | `server_name` 改为 `pay.zyapay.com`，证书路径同步更新，清理重复 server block |
| application-prod.yml | `shopline.login-base-url: https://pay.zyapay.com` |
| application-prod.yml | `shopline.redirect-uri: https://pay.zyapay.com/api/pay/shopline/callback` |
| 下单回调 URL | `notifyUrl` / `redirectUrl` / `cancelUrl` 全部换成 `pay.zyapay.com` |
| Shopline 后台 | 更新 App 回调域名白名单 |
| 防火墙 | 确认 443 和 80 端口对外开放（80 用于证书续期 HTTP-01 验证） |
| 证书续期 hook | 确认 reload-nginx.sh 存在且可执行 |