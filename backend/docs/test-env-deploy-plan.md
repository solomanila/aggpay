# 测试环境部署计划（单台 VPS，无容器化）

## 前提条件

VPS 上已安装并运行：
- MySQL 8（密码：`905088@aB`，已建库 `admin` / `aggpay` / `letsvpn`）
- Redis 7（密码：`foobaredsolo123`）
- Nacos 2.x（`127.0.0.1:8848`，standalone 模式）
- RocketMQ（`127.0.0.1:9876`）

**无需 Docker / k3s / k8s。**

---

## 服务清单

| 服务 | 启动端口 | 依赖 DB |
|------|---------|---------|
| gateway | 8080 | 无 |
| auth-service | 8081 | letsvpn |
| pay-service | 8084 | aggpay |
| admin-service | 8085 | admin |
| user-service | 8082 | letsvpn（可选，暂跳过）|

---

## 第一步：本地打包（Windows 开发机）

```bash
# 在 backend/ 目录下执行，跳过测试，打出所有 fat JAR
cd backend
mvn package -DskipTests -q

# 打包完成后，各服务 JAR 路径：
# backend/gateway/target/gateway-*.jar
# backend/auth-service/target/auth-service-*.jar
# backend/pay-service/target/pay-service-*.jar
# backend/admin-service/target/admin-service-*.jar
```

## 第二步：本地构建前端

```bash
# 在项目根目录（payadmin-ui/）执行
# 使用 test 模式：VITE_API_BASE_URL 为空，运行时自动取 window.location.origin/api
# 前后端同机 + Nginx 反代，无需硬编码 IP
npm run build -- --mode test
# 产物在 dist/ 目录
```

---

## 第三步：上传到 VPS

```bash
# 在 Windows 本地执行（PowerShell 或 Git Bash）
VPS=user@VPS_IP

# 上传 JAR（每个服务单独上传，按需选择）
scp backend/gateway/target/gateway-*.jar          $VPS:~/apps/gateway.jar
scp backend/auth-service/target/auth-service-*.jar $VPS:~/apps/auth-service.jar
scp backend/pay-service/target/pay-service-*.jar   $VPS:~/apps/pay-service.jar
scp backend/admin-service/target/admin-service-*.jar $VPS:~/apps/admin-service.jar

# 上传前端产物
scp -r dist/ $VPS:~/apps/frontend/
```

---

## 第四步：VPS 环境准备

```bash
# 检查 Java 版本（需要 17+）
java -version

# 若未安装，执行：
sudo apt install -y openjdk-17-jre-headless

# 创建工作目录
mkdir -p ~/apps/logs
```

---

## 第五步：启动后端服务

所有服务使用 `-Dspring.profiles.active=test` 加载 `application-test.yml`，
该配置已指向 `127.0.0.1` 上的中间件，且 Redis 密码已配置为 `foobaredsolo123`。

### 启动脚本 `~/apps/start-all.sh`

在 VPS 上创建此文件：

```bash
cat > ~/apps/start-all.sh << 'EOF'
#!/bin/bash
JAVA_OPTS="-Xms256m -Xmx512m -Dspring.profiles.active=test"
LOG_DIR=~/apps/logs

start() {
  local name=$1
  local jar=$2
  local port=$3
  echo "Starting $name on port $port ..."
  nohup java $JAVA_OPTS -jar ~/apps/$jar > $LOG_DIR/$name.log 2>&1 &
  echo $! > $LOG_DIR/$name.pid
  sleep 2
}

# 按依赖顺序启动
start auth-service  auth-service.jar  8081
start pay-service   pay-service.jar   8084
start admin-service admin-service.jar 8085
start gateway       gateway.jar       8080

echo "All services started. Check logs in $LOG_DIR/"
EOF
chmod +x ~/apps/start-all.sh
```

### 停止脚本 `~/apps/stop-all.sh`

```bash
cat > ~/apps/stop-all.sh << 'EOF'
#!/bin/bash
for pid_file in ~/apps/logs/*.pid; do
  pid=$(cat "$pid_file")
  name=$(basename "$pid_file" .pid)
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid" && echo "Stopped $name (PID $pid)"
  fi
  rm -f "$pid_file"
done
EOF
chmod +x ~/apps/stop-all.sh
```

### 执行启动

```bash
~/apps/start-all.sh

# 查看启动日志（等待约 30s）
tail -f ~/apps/logs/gateway.log
tail -f ~/apps/logs/admin-service.log
```

---

## 第六步：配置 Nginx 服务前端

```bash
sudo apt install -y nginx
```

创建站点配置 `/etc/nginx/sites-available/payadmin`：

```nginx
server {
    listen 80;
    server_name _;

    root /home/YOUR_USER/apps/frontend;
    index index.html;

    # Vue SPA 路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 反向代理到 gateway
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_read_timeout 60s;
    }
}
```

```bash
# 将 YOUR_USER 替换为实际用户名
sudo ln -s /etc/nginx/sites-available/payadmin /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

---

## 访问地址

| 地址 | 说明 |
|------|------|
| `http://VPS_IP` | 前端管理页面 |
| `http://VPS_IP:8080/api` | Gateway（直接调试 API 用）|
| `http://VPS_IP:8848/nacos` | Nacos 控制台 |

---

## 防火墙开放端口

```bash
sudo ufw allow 80/tcp    # 前端
sudo ufw allow 8080/tcp  # Gateway（可选，调试用）
sudo ufw allow 8848/tcp  # Nacos 控制台（可选，调试用）
sudo ufw reload
```

---

## 排障速查

```bash
# 查看某服务实时日志
tail -f ~/apps/logs/admin-service.log

# 检查端口监听
ss -tlnp | grep -E '8080|8081|8084|8085'

# 检查服务是否注册到 Nacos（浏览器打开）
http://VPS_IP:8848/nacos → 服务管理 → 服务列表

# Redis 连通性验证
redis-cli -a foobaredsolo123 ping

# 重启单个服务（以 admin-service 为例）
kill $(cat ~/apps/logs/admin-service.pid)
nohup java -Xms256m -Xmx512m -Dspring.profiles.active=test \
  -jar ~/apps/admin-service.jar > ~/apps/logs/admin-service.log 2>&1 &
```

---

## 执行清单

- [ ] 本地 `mvn package -DskipTests` 打包全部服务
- [ ] 本地 `npm run build -- --mode test` 构建前端（无需替换 IP）
- [ ] `scp` 上传 JAR + `dist/` 到 VPS `~/apps/`
- [ ] VPS 确认 Java 17 已安装
- [ ] VPS 创建并执行 `start-all.sh`
- [ ] VPS 配置 Nginx，`systemctl reload nginx`
- [ ] 开放防火墙端口
- [ ] 浏览器访问 `http://VPS_IP` 验证登录流程
