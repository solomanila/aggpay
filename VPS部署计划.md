# 部署到 VPS（k3s 机器）- 最小工作量方案

## Context
本地 Windows 开发机已成功运行前端（Vue/Vite）+ 后端（5个 Spring Boot 微服务）。  
目标：让他人通过测试 VPS 的 IP 访问系统，查看开发进度。  
VPS 已安装 k3s，但 **不使用 k8s manifests**，而是在 VPS 上额外安装 Docker，用 **docker compose** 一条命令启动所有服务。这是最小工作量路径。

## 方案选择依据
| 方案 | 工作量 | 复杂度 |
|------|--------|--------|
| Docker Compose（推荐）| **最少**（新建 5 个文件）| 低 |
| k3s manifests | 高（需要写 10+ yaml）| 高 |
| 纯 k8s Helm | 极高 | 极高 |

Spring Boot 支持环境变量直接覆盖 yml 配置（如 `SPRING_DATASOURCE_URL`），因此**无需修改任何 application.yml**。

---

## 需要的中间件
| 中间件 | 用途 | 容器镜像 |
|--------|------|---------|
| MySQL 8 | 存储（4个DB：letsvpn / aggpay / admin / nacos_config）| mysql:8.0 |
| Redis | 缓存 / 限流 | redis:7-alpine |
| Nacos 2.2.3 | 服务注册与发现 | nacos/nacos-server:v2.2.3 |
| RocketMQ | pay-service 消息队列 | apache/rocketmq:5.1.0 |

## 需要运行的后端服务
| 服务 | 端口 | 说明 |
|------|------|------|
| gateway | 8080 | 统一入口，对外暴露 |
| auth-service | 8081 | 登录认证 |
| pay-service | 8084 | 订单/通道数据（aggpay DB）|
| admin-service | 8085 | 管理后台 API（admin DB）|
| user-service | 8082 | 可选，VPN 功能，暂可跳过 |

---

## 要创建的文件（共 5 个，全部新建）

### 1. `Dockerfile.frontend`（项目根目录，与 package.json 同级）

```dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# 构建时注入 VPS 地址
ARG VITE_API_BASE_URL=http://VPS_IP:8080/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

### 2. `nginx.conf`（项目根目录）

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://gateway:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 3. `docker-compose.yml`（项目根目录）

```yaml
version: '3.9'

x-java-env: &java-env
  SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: nacos:8848
  SPRING_DATA_REDIS_HOST: redis
  SPRING_REDIS_HOST: redis

services:
  # ── 中间件 ──────────────────────────────────────────
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: "905088"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./backend/docs/init.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-p905088"]
      interval: 10s
      retries: 10

  redis:
    image: redis:7-alpine
    volumes:
      - redis-data:/data

  nacos:
    image: nacos/nacos-server:v2.2.3
    environment:
      MODE: standalone
      SPRING_DATASOURCE_PLATFORM: mysql
      MYSQL_SERVICE_HOST: mysql
      MYSQL_SERVICE_PORT: 3306
      MYSQL_SERVICE_DB_NAME: nacos_config
      MYSQL_SERVICE_USER: root
      MYSQL_SERVICE_PASSWORD: "905088"
    ports:
      - "8848:8848"
    depends_on:
      mysql:
        condition: service_healthy

  rocketmq-namesrv:
    image: apache/rocketmq:5.1.0
    command: sh mqnamesrv
    environment:
      JAVA_OPT_EXT: "-Xms256m -Xmx256m"

  rocketmq-broker:
    image: apache/rocketmq:5.1.0
    command: sh mqbroker -n rocketmq-namesrv:9876
    environment:
      JAVA_OPT_EXT: "-Xms256m -Xmx256m"
    depends_on:
      - rocketmq-namesrv

  # ── 后端服务 ─────────────────────────────────────────
  auth-service:
    build:
      context: ./backend
      dockerfile: Dockerfile.auth-service
    environment:
      <<: *java-env
      SPRING_DATASOURCE_URL: "jdbc:mysql://mysql:3306/letsvpn?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: "905088"
      NACOS_SERVER: nacos:8848
    depends_on:
      nacos:
        condition: service_started

  pay-service:
    build:
      context: ./backend
      dockerfile: Dockerfile.pay-service
    environment:
      <<: *java-env
      SPRING_DATASOURCE_URL: "jdbc:mysql://mysql:3306/aggpay?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: "905088"
      NACOS_SERVER: nacos:8848
      ROCKETMQ_NAME_SERVER: rocketmq-namesrv:9876
    depends_on:
      nacos:
        condition: service_started
      rocketmq-namesrv:
        condition: service_started

  admin-service:
    build:
      context: ./backend
      dockerfile: Dockerfile.admin-service
    environment:
      <<: *java-env
      SPRING_DATASOURCE_URL: "jdbc:mysql://mysql:3306/admin?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: "905088"
      NACOS_SERVER: nacos:8848
    depends_on:
      nacos:
        condition: service_started

  gateway:
    build:
      context: ./backend
      dockerfile: Dockerfile.gateway
    environment:
      <<: *java-env
      NACOS_SERVER: nacos:8848
    ports:
      - "8080:8080"
    depends_on:
      auth-service:
        condition: service_started
      pay-service:
        condition: service_started
      admin-service:
        condition: service_started

  frontend:
    build:
      context: .
      dockerfile: Dockerfile.frontend
      args:
        VITE_API_BASE_URL: http://VPS_IP:8080/api   # ← 替换为实际 VPS IP
    ports:
      - "80:80"
    depends_on:
      - gateway

volumes:
  mysql-data:
  redis-data:
```

### 4. `backend/docs/init.sql`（MySQL 初始化脚本）

```sql
CREATE DATABASE IF NOT EXISTS `letsvpn`  CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS `aggpay`   CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS `admin`    CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS `nacos_config` CHARACTER SET utf8mb4;
```

### 5. `backend/Dockerfile.admin-service`（目前缺少此文件，其余 4 个已有）

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common-core/pom.xml common-core/
COPY common-data/pom.xml common-data/
COPY admin-service/pom.xml admin-service/
RUN mvn dependency:go-offline -pl admin-service -am -q
COPY . .
RUN mvn package -pl admin-service -am -DskipTests -q

FROM eclipse-temurin:17-jre-focal
WORKDIR /app
COPY --from=build /workspace/admin-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## VPS 部署步骤（在 VPS 上执行）

```bash
# 1. 安装 Docker（k3s 不含 docker，需单独装）
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER && newgrp docker

# 2. 把代码传到 VPS（选一种）
# 方法A: scp 整个目录（Windows 本地执行）
scp -r D:/ideaprojects/payadmin-ui user@VPS_IP:~/payadmin-ui

# 方法B: git clone（若有 git 仓库）
git clone <repo-url> payadmin-ui

# 3. 修改 VPS IP（必须，替换为实际 IP）
cd ~/payadmin-ui
sed -i 's/VPS_IP/实际IP地址/g' docker-compose.yml

# 4. 启动所有服务（首次构建约 10-20 分钟）
docker compose up --build -d

# 5. 查看状态
docker compose ps
docker compose logs -f gateway
```

## 访问地址
- 前端：`http://VPS_IP:80`
- Gateway API：`http://VPS_IP:8080`
- Nacos 控制台：`http://VPS_IP:8848/nacos`（用于调试）

---

## 关键注意事项

1. **Nacos 地址覆盖（重要）**：admin-service 的 bootstrap.yml 已支持 `${NACOS_SERVER:127.0.0.1:8848}`，但 gateway / auth-service / pay-service 的 bootstrap.yml 可能硬编码了 `127.0.0.1:8848`。需要检查这三个服务的 bootstrap.yml，若硬编码则修改为 `${NACOS_SERVER:127.0.0.1:8848}`，否则容器内服务无法注册到 Nacos。

2. **VPS 内存**：所有服务 + 中间件约需 4-6 GB RAM，请确认 VPS 配置。

3. **防火墙**：VPS 需开放端口：
   - `80` — 前端
   - `8080` — Gateway（可选，直接调试 API 用）
   - `8848` — Nacos 控制台（可选，调试用）

4. **数据持久化**：MySQL / Redis 已配置 Docker volume，`docker compose down` 不丢数据；`docker compose down -v` 才会清空数据。

5. **明天开始执行顺序**：
   - [ ] 创建上述 5 个文件（内容已在本文档中）
   - [ ] 检查并修改 gateway/auth-service/pay-service 的 bootstrap.yml（Nacos 地址参数化）
   - [ ] 将项目传至 VPS，替换 `VPS_IP` 为实际地址
   - [ ] `docker compose up --build -d`
