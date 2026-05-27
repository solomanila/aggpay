# Jenkins CI/CD 流水线搭建指南

基于现有生产环境（AWS ap-south-1，私网无外网，镜像从 gateway 中转）搭建自动化发布流水线。

---

## 环境概览

| 角色 | 内网 IP | 说明 |
|------|---------|------|
| gateway（Jenkins 宿主） | 10.0.1.30（公网 EIP: 13.234.6.115） | 有公网，ECR 访问入口，SSH 跳板 |
| auth-service | 10.0.1.29 | 私网，无外网 |
| pay-service-1 | 10.0.1.167 | 私网，无外网 |
| pay-service-2 | 10.0.1.19 | 私网，无外网 |
| admin-service | 10.0.1.38 | 私网，无外网 |
| ECR | 289400855294.dkr.ecr.ap-south-1.amazonaws.com | 镜像仓库 |

---

## Phase 1：半自动化部署脚本（30 分钟落地）

不依赖 Jenkins，push 镜像到 ECR 后手动触发部署脚本。适合立即使用。

### 1.1 创建目录

```bash
# 在 gateway（10.0.1.30）上执行
sudo mkdir -p /opt/deploy
sudo chown ec2-user:ec2-user /opt/deploy
```

### 1.2 后端部署脚本

创建 `/opt/deploy/deploy.sh`：

```bash
#!/bin/bash
set -e

ECR_REGISTRY="289400855294.dkr.ecr.ap-south-1.amazonaws.com"
REGION="ap-south-1"
KEY="$HOME/aggpay_key.pem"

SERVICE=$1
if [ -z "$SERVICE" ]; then
  echo "Usage: ./deploy.sh <auth-service|pay-service|admin-service|gateway>"
  exit 1
fi

ecr_login() {
  aws ecr get-login-password --region $REGION \
    | sudo docker login --username AWS --password-stdin $ECR_REGISTRY
}

transfer_image() {
  local image=$1
  local target_ip=$2
  echo ">>> 传输 $image 到 $target_ip ..."
  sudo docker save "$image" \
    | gzip \
    | ssh -i $KEY -o StrictHostKeyChecking=no ec2-user@$target_ip "gunzip | sudo docker load"
}

restart_container() {
  local target_ip=$1
  local service=$2
  local run_cmd=$3
  echo ">>> 重启 $service @ $target_ip ..."
  ssh -i $KEY -o StrictHostKeyChecking=no ec2-user@$target_ip \
    "sudo docker stop $service 2>/dev/null || true; sudo docker rm $service 2>/dev/null || true; $run_cmd"
}

IMAGE="$ECR_REGISTRY/payadmin/$SERVICE:latest"

case $SERVICE in
  auth-service)
    ecr_login
    sudo docker pull $IMAGE
    transfer_image $IMAGE 10.0.1.29
    restart_container 10.0.1.29 auth-service \
      "sudo docker run -d --name auth-service --restart always --network host \
       -e SPRING_PROFILES_ACTIVE=prod \
       -e NACOS_SERVER=10.0.1.130:8848 \
       -e SPRING_CLOUD_NACOS_CONFIG_ENABLED=false \
       -e MYSQL_HOST=10.0.1.176 -e MYSQL_USERNAME=root -e MYSQL_PASSWORD=Payadmin@2024 \
       -e REDIS_HOST=10.0.1.130 -e REDIS_PASSWORD=Payadmin@2024 \
       -e TZ=Asia/Kolkata $IMAGE"
    ;;

  pay-service)
    ecr_login
    sudo docker pull $IMAGE
    transfer_image $IMAGE 10.0.1.167
    transfer_image $IMAGE 10.0.1.19
    PAY_RUN="sudo docker run -d --name pay-service --restart always --network host \
       -e SPRING_PROFILES_ACTIVE=prod \
       -e NACOS_SERVER=10.0.1.130:8848 \
       -e SPRING_CLOUD_NACOS_CONFIG_ENABLED=false \
       -e MYSQL_HOST=10.0.1.176 -e MYSQL_USERNAME=root -e MYSQL_PASSWORD=Payadmin@2024 \
       -e REDIS_HOST=10.0.1.130 -e REDIS_PASSWORD=Payadmin@2024 \
       -e ROCKETMQ_NAME_SERVER=10.0.1.54:9876 \
       -e TELEGRAM_BOT_TOKEN=8360749651:AAEoN0ieATm7xqNvyRuqmqxMegG2SgYbdXA \
       -e 'JAVA_TOOL_OPTIONS=-Dhttps.proxyHost=10.0.1.30 -Dhttps.proxyPort=3128 -Dhttp.proxyHost=10.0.1.30 -Dhttp.proxyPort=3128 -Dhttp.nonProxyHosts=10.0.1.*|localhost|127.* -Dhttps.nonProxyHosts=10.0.1.*|localhost|127.*' \
       -e TZ=Asia/Kolkata $IMAGE"
    # 滚动更新：先停实例2，等 Nacos 重新注册，再停实例1
    restart_container 10.0.1.19 pay-service "$PAY_RUN"
    echo ">>> 等待实例2重新注册 Nacos (30s)..."
    sleep 30
    restart_container 10.0.1.167 pay-service "$PAY_RUN"
    ;;

  admin-service)
    ecr_login
    sudo docker pull $IMAGE
    transfer_image $IMAGE 10.0.1.38
    restart_container 10.0.1.38 admin-service \
      "sudo docker run -d --name admin-service --restart always --network host \
       -e SPRING_PROFILES_ACTIVE=prod \
       -e NACOS_SERVER=10.0.1.130:8848 \
       -e SPRING_CLOUD_NACOS_CONFIG_ENABLED=false \
       -e MYSQL_HOST=10.0.1.176 -e MYSQL_USERNAME=root -e MYSQL_PASSWORD=Payadmin@2024 \
       -e REDIS_HOST=10.0.1.130 -e REDIS_PASSWORD=Payadmin@2024 \
       -e ROCKETMQ_NAME_SERVER=10.0.1.54:9876 \
       -e TZ=Asia/Kolkata $IMAGE"
    ;;

  gateway)
    ecr_login
    sudo docker pull $IMAGE
    sudo docker stop gateway 2>/dev/null || true
    sudo docker rm gateway 2>/dev/null || true
    sudo docker run -d --name gateway --restart always --network host \
       -e SPRING_PROFILES_ACTIVE=prod \
       -e NACOS_SERVER=10.0.1.130:8848 \
       -e SPRING_CLOUD_NACOS_CONFIG_ENABLED=false \
       -e REDIS_HOST=10.0.1.130 -e REDIS_PASSWORD=Payadmin@2024 \
       -e TZ=Asia/Kolkata \
       $IMAGE
    ;;

  *)
    echo "未知服务: $SERVICE"
    exit 1
    ;;
esac

echo "✅ $SERVICE 部署完成"
```

```bash
chmod +x /opt/deploy/deploy.sh
```

### 1.3 前端部署脚本

在本地 Windows 开发机（Git Bash / WSL）创建 `deploy-frontend.sh`：

```bash
#!/bin/bash
set -e
cd D:/ideaprojects/payadmin-ui
npm run build
scp -i D:/notes/aggpay_key.pem -r dist/* ec2-user@13.234.6.115:/tmp/frontend_dist/
ssh -i D:/notes/aggpay_key.pem ec2-user@13.234.6.115 \
  "sudo cp -r /tmp/frontend_dist/* /usr/share/nginx/html/ && sudo systemctl reload nginx"
echo "✅ 前端部署完成"
```

### 1.4 Phase 1 使用方式

```bash
# 本地构建并 push 镜像到 ECR（本地执行）
cd D:\ideaprojects\payadmin-ui\backend
docker build -f Dockerfile.admin-service -t 289400855294.dkr.ecr.ap-south-1.amazonaws.com/payadmin/admin-service:latest .
aws ecr get-login-password --region ap-south-1 | docker login --username AWS --password-stdin 289400855294.dkr.ecr.ap-south-1.amazonaws.com
docker push 289400855294.dkr.ecr.ap-south-1.amazonaws.com/payadmin/admin-service:latest

# SSH 进 gateway，触发部署（gateway 上执行）
ssh -i D:/notes/aggpay_key.pem ec2-user@13.234.6.115
/opt/deploy/deploy.sh admin-service

# 前端（本地执行）
bash deploy-frontend.sh
```

---

## Phase 2：Jenkins 全自动流水线

### 2.1 在 gateway 安装 Jenkins

> ⚠️ **实测坑点**：
> - 新版 Jenkins（2.357+）**最低要求 Java 21**，Java 17 会报错退出，必须安装 `java-21-amazon-corretto`
> - 新版 Jenkins RPM **不再生成 `/etc/sysconfig/jenkins`**，需改用 systemd override 配置端口
> - gateway 上默认没有安装 `git`，需手动安装，否则 Pipeline 拉取代码会失败

```bash
# 安装 Java 21（新版 Jenkins 要求，Java 17 不支持）
sudo dnf install -y java-21-amazon-corretto

# 安装 git（Jenkins Pipeline 拉取代码必须）
sudo dnf install -y git

# 添加 Jenkins 仓库并安装
sudo wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat-stable/jenkins.repo
sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io-2023.key
sudo dnf install -y jenkins

# 修改端口为 8888（新版无 /etc/sysconfig/jenkins，用 systemd override）
sudo mkdir -p /etc/systemd/system/jenkins.service.d
sudo tee /etc/systemd/system/jenkins.service.d/override.conf << 'EOF'
[Service]
Environment="JENKINS_PORT=8888"
EOF

# 启动并设置开机自启
sudo systemctl daemon-reload
sudo systemctl start jenkins
sudo systemctl enable jenkins

# 查看初始密码
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

访问 `http://13.234.6.115:8888` 完成初始化向导。

> **安全组**：在 gateway 实例（非 VPC ACL）的 Security Group 开放 `8888/TCP`，多个运维 IP 各加一条规则，来源填 `x.x.x.x/32`（精确匹配单 IP），上线后可关闭（使用 SSH 隧道访问）。

### 2.2 安装必要插件

进入 `Manage Jenkins → Plugins → Available`，安装以下插件：

| 插件 | 用途 |
|------|------|
| Pipeline | 流水线核心 |
| Git | 拉取代码 |
| Docker Pipeline | Docker 构建/推送 |
| SSH Agent | SSH 连接私网机器 |
| Blue Ocean | 可视化 UI（推荐） |
| Build Timeout | 防止构建卡死 |
| NodeJS | 前端构建（npm） |

安装完勾选 **Restart Jenkins when installation is complete**，等待自动重启后重新登录。

### 2.3 配置凭据

进入 `Manage Jenkins → Credentials → System → Global credentials (unrestricted) → Add Credentials`，添加：

| ID | 类型 | 内容 |
|----|------|------|
| `aws-ecr-key` | AWS Credentials | ECR 登录（推荐用 IAM Role，gateway 已有则无需添加） |
| `deploy-ssh-key` | SSH Username with private key | Username 填 `ec2-user`，选 Enter directly，粘贴 aggpay_key.pem 完整内容 |
| `github-token` | Username with password | Username 填 GitHub 用户名，Password 填 GitHub PAT（见下方说明） |

#### GitHub 私有仓库授权（Personal Access Token）

1. GitHub → `Settings` → `Developer settings` → `Personal access tokens` → `Tokens (classic)` → `Generate new token (classic)`
2. 勾选 **`repo`** 权限（包含私有仓库读写）
3. 生成后立即复制（只显示一次），填入 Jenkins 凭据的 Password 字段

### 2.4 配置 Maven 和 Node.js 工具

进入 `Manage Jenkins → Tools`：

- **JDK**：名称 `jdk21`，**取消勾选自动安装**，JAVA_HOME 填 `/usr/lib/jvm/java-21-amazon-corretto.x86_64`
- **Maven**：名称 `maven3`，勾选自动安装，版本选最新 `3.9.x`
- **NodeJS**：名称 `node18`，勾选自动安装，版本选 `18.20.x`

### 2.5 创建 Pipeline Job

1. 新建 Item → 选 **Pipeline**，命名 `payadmin-deploy`
2. **General** 勾选 `This project is parameterized` → Add Parameter → **Choice Parameter**，Name 填 `DEPLOY_TARGET`，Choices 每行一个：`all` / `backend-only` / `frontend-only` / `pay-service` / `admin-service` / `auth-service` / `gateway`
3. **Build Triggers** 勾选 `GitHub hook trigger for GITScm polling`
4. **Pipeline** → Definition 选 `Pipeline script from SCM` → SCM 选 `Git`
   - Repository URL：`https://github.com/solomanila/aggpay.git`
   - Credentials：选 `github-token`
   - Branch：`*/main`
   - Script Path：`Jenkinsfile`

### 2.6 Jenkinsfile

在项目根目录创建 `Jenkinsfile`：

```groovy
pipeline {
    agent any

    environment {
        ECR_REGISTRY = '289400855294.dkr.ecr.ap-south-1.amazonaws.com'
        REGION       = 'ap-south-1'
        KEY_PATH     = '/home/ec2-user/aggpay_key.pem'
    }

    parameters {
        choice(
            name: 'DEPLOY_TARGET',
            choices: ['all', 'backend-only', 'frontend-only',
                      'pay-service', 'admin-service', 'auth-service', 'gateway'],
            description: '选择部署范围'
        )
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Maven Build') {
            when {
                expression {
                    params.DEPLOY_TARGET in ['all', 'backend-only',
                        'pay-service', 'admin-service', 'auth-service', 'gateway']
                }
            }
            tools { maven 'maven3' }
            steps {
                dir('backend') {
                    sh 'mvn clean package -DskipTests -pl gateway,auth-service,pay-service,admin-service --also-make'
                }
            }
        }

        stage('Docker Build & Push to ECR') {
            when {
                expression {
                    params.DEPLOY_TARGET in ['all', 'backend-only',
                        'pay-service', 'admin-service', 'auth-service', 'gateway']
                }
            }
            steps {
                script {
                    sh """
                        aws ecr get-login-password --region ${REGION} \
                          | sudo docker login --username AWS --password-stdin ${ECR_REGISTRY}
                    """
                    def services = []
                    if (params.DEPLOY_TARGET in ['all', 'backend-only']) {
                        services = ['auth-service', 'pay-service', 'admin-service', 'gateway']
                    } else {
                        services = [params.DEPLOY_TARGET]
                    }
                    services.each { svc ->
                        sh """
                            sudo docker build -f backend/Dockerfile.${svc} \
                              -t ${ECR_REGISTRY}/payadmin/${svc}:latest \
                              -t ${ECR_REGISTRY}/payadmin/${svc}:build-${BUILD_NUMBER} \
                              backend/
                            sudo docker push ${ECR_REGISTRY}/payadmin/${svc}:latest
                            sudo docker push ${ECR_REGISTRY}/payadmin/${svc}:build-${BUILD_NUMBER}
                        """
                    }
                }
            }
        }

        stage('Deploy auth-service') {
            when {
                expression { params.DEPLOY_TARGET in ['all', 'backend-only', 'auth-service'] }
            }
            steps {
                script { deployBackend('auth-service', '10.0.1.29') }
            }
        }

        stage('Deploy pay-service (Rolling)') {
            when {
                expression { params.DEPLOY_TARGET in ['all', 'backend-only', 'pay-service'] }
            }
            steps {
                script {
                    // 先更新实例2，等 Nacos 重新注册后再更新实例1，保证不停服
                    deployBackend('pay-service', '10.0.1.19')
                    sleep(time: 30, unit: 'SECONDS')
                    deployBackend('pay-service', '10.0.1.167')
                }
            }
        }

        stage('Deploy admin-service') {
            when {
                expression { params.DEPLOY_TARGET in ['all', 'backend-only', 'admin-service'] }
            }
            steps {
                script { deployBackend('admin-service', '10.0.1.38') }
            }
        }

        stage('Deploy gateway') {
            when {
                expression { params.DEPLOY_TARGET in ['all', 'backend-only', 'gateway'] }
            }
            steps {
                sh """
                    sudo docker pull ${ECR_REGISTRY}/payadmin/gateway:latest
                    sudo docker stop gateway 2>/dev/null || true
                    sudo docker rm gateway 2>/dev/null || true
                    sudo docker run -d --name gateway --restart always --network host \
                      -e SPRING_PROFILES_ACTIVE=prod \
                      -e NACOS_SERVER=10.0.1.130:8848 \
                      -e SPRING_CLOUD_NACOS_CONFIG_ENABLED=false \
                      -e REDIS_HOST=10.0.1.130 -e REDIS_PASSWORD=Payadmin@2024 \
                      -e TZ=Asia/Kolkata \
                      ${ECR_REGISTRY}/payadmin/gateway:latest
                """
            }
        }

        stage('Frontend Build & Deploy') {
            when {
                expression { params.DEPLOY_TARGET in ['all', 'frontend-only'] }
            }
            tools { nodejs 'node18' }
            steps {
                sh 'npm ci && npm run build'
                sh 'sudo cp -r dist/* /usr/share/nginx/html/ && sudo systemctl reload nginx'
            }
        }

        stage('Health Check') {
            steps {
                script {
                    sleep(time: 15, unit: 'SECONDS')
                    sh '''
                        echo "=== Nacos 注册情况 ==="
                        curl -sf "http://10.0.1.130:8848/nacos/v1/ns/instance/list?serviceName=pay-service" \
                          | python3 -m json.tool | grep -E '"ip"|"healthy"' || true
                        echo "=== Gateway 探活 ==="
                        curl -sf -o /dev/null -w "HTTP %{http_code}\\n" http://127.0.0.1:8080/actuator/health \
                          || echo "（gateway 未配置 actuator，跳过）"
                    '''
                }
            }
        }
    }

    post {
        success {
            echo "✅ 部署成功 - Build #${BUILD_NUMBER}"
        }
        failure {
            echo "❌ 部署失败 - 请查看 Jenkins 控制台日志"
        }
    }
}

// ── 通用方法：传镜像 + 重启容器 ──────────────────────────────────────────
def deployBackend(String service, String targetIp) {
    def image = "${env.ECR_REGISTRY}/payadmin/${service}:latest"
    sh """
        sudo docker save ${image} \
          | gzip \
          | ssh -i ${env.KEY_PATH} -o StrictHostKeyChecking=no ec2-user@${targetIp} \
              "gunzip | sudo docker load"
    """
    def runCmd = getRunCommand(service, image)
    sh """
        ssh -i ${env.KEY_PATH} -o StrictHostKeyChecking=no ec2-user@${targetIp} \
          "sudo docker stop ${service} 2>/dev/null || true; \
           sudo docker rm ${service} 2>/dev/null || true; \
           ${runCmd}"
    """
}

def getRunCommand(String service, String image) {
    def base    = "sudo docker run -d --name ${service} --restart always --network host -e TZ=Asia/Kolkata"
    def common  = "-e SPRING_PROFILES_ACTIVE=prod -e NACOS_SERVER=10.0.1.130:8848 -e SPRING_CLOUD_NACOS_CONFIG_ENABLED=false -e MYSQL_HOST=10.0.1.176 -e MYSQL_USERNAME=root -e MYSQL_PASSWORD=Payadmin@2024 -e REDIS_HOST=10.0.1.130 -e REDIS_PASSWORD=Payadmin@2024"
    def proxy   = "-e 'JAVA_TOOL_OPTIONS=-Dhttps.proxyHost=10.0.1.30 -Dhttps.proxyPort=3128 -Dhttp.proxyHost=10.0.1.30 -Dhttp.proxyPort=3128 -Dhttp.nonProxyHosts=10.0.1.*|localhost|127.* -Dhttps.nonProxyHosts=10.0.1.*|localhost|127.*'"
    switch(service) {
        case 'auth-service':
            return "${base} ${common} ${image}"
        case 'pay-service':
            return "${base} ${common} -e ROCKETMQ_NAME_SERVER=10.0.1.54:9876 -e TELEGRAM_BOT_TOKEN=8360749651:AAEoN0ieATm7xqNvyRuqmqxMegG2SgYbdXA ${proxy} ${image}"
        case 'admin-service':
            return "${base} ${common} -e ROCKETMQ_NAME_SERVER=10.0.1.54:9876 ${image}"
        default:
            return "${base} ${image}"
    }
}
```

### 2.7 配置 Git Webhook

在 GitHub / Gitea 仓库设置中添加 Webhook：

```
Payload URL: http://13.234.6.115:8888/github-webhook/
Content type: application/json
触发事件: Just the push event
```

> 推送到 `main` 分支时自动触发，默认部署全部服务（`all`）。

---

## SSH 隧道访问 Jenkins（不暴露公网）

不需要对外开放 8888 端口，通过 SSH 隧道本地访问：

```bash
# 在本地 Windows 执行（Git Bash）
ssh -i D:/notes/aggpay_key.pem -L 8888:127.0.0.1:8888 ec2-user@13.234.6.115 -N
# 然后浏览器打开 http://localhost:8888
```

---

## 回滚操作

每次构建会额外打一个带编号的 tag（`build-N`），回滚时指定该 tag：

```bash
# 在 gateway 上，回滚 admin-service 到第 5 次构建
IMAGE="289400855294.dkr.ecr.ap-south-1.amazonaws.com/payadmin/admin-service:build-5"
sudo docker pull $IMAGE
sudo docker save $IMAGE | gzip | ssh -i ~/aggpay_key.pem ec2-user@10.0.1.38 "gunzip | sudo docker load"
ssh -i ~/aggpay_key.pem ec2-user@10.0.1.38 \
  "sudo docker stop admin-service && sudo docker rm admin-service && \
   sudo docker run -d --name admin-service --restart always --network host \
   -e SPRING_PROFILES_ACTIVE=prod -e NACOS_SERVER=10.0.1.130:8848 \
   -e SPRING_CLOUD_NACOS_CONFIG_ENABLED=false \
   -e MYSQL_HOST=10.0.1.176 -e MYSQL_USERNAME=root -e MYSQL_PASSWORD=Payadmin@2024 \
   -e REDIS_HOST=10.0.1.130 -e REDIS_PASSWORD=Payadmin@2024 \
   -e ROCKETMQ_NAME_SERVER=10.0.1.54:9876 \
   -e TZ=Asia/Kolkata $IMAGE"
```

---

## 方案对比

| | Phase 1（脚本） | Phase 2（Jenkins） |
|--|--|--|
| 搭建时间 | 30 分钟 | 1-2 天 |
| 触发方式 | 手动 SSH 执行 | git push 自动触发 |
| 构建历史 | 无 | 完整日志 + build 编号 |
| 版本回滚 | 手动指定 image tag | 一键回滚到任意 build-N |
| 可视化 | 无 | Blue Ocean UI |
| 新增成本 | 0 | 占用 gateway 约 1 GB RAM |

**建议**：先用 Phase 1 脚本验证流程可用性，同周内完成 Jenkins 搭建实现全自动化。

---

## 实操踩坑记录

### Windows 本地 SCP/SSH 命令

- PowerShell **不支持 `\` 续行**，多行命令必须写成单行或用反引号 `` ` `` 续行
- Windows OpenSSH 对 `.pem` 文件权限严格，若提示 `UNPROTECTED PRIVATE KEY FILE` 执行：
  ```powershell
  icacls D:\notes\aggpay_key.pem /inheritance:r /grant:r "$($env:USERNAME):(R)"
  ```
- 推荐使用 **FinalShell** 的 SFTP 面板上传文件，避开 Windows SSH 权限问题

### Jenkins 安装（Amazon Linux 2023）

| 问题 | 原因 | 解决 |
|------|------|------|
| Jenkins 启动失败，报 `Java 17 older than minimum` | 新版 Jenkins 要求 Java 21 | 安装 `java-21-amazon-corretto` |
| `sed: can't read /etc/sysconfig/jenkins` | 新版 RPM 不再生成该文件 | 改用 systemd override 设置端口 |
| Pipeline 报 `git ls-remote` 失败 | gateway 未安装 git | `sudo dnf install -y git` |

### Jenkins 用户权限初始化（必做）

首次搭建完成后，在 gateway 上执行以下初始化命令，否则 Pipeline 会因权限不足失败：

```bash
# 1. 给 jenkins 用户免密 sudo（否则 docker 命令全部失败）
echo "jenkins ALL=(ALL) NOPASSWD:ALL" | sudo tee /etc/sudoers.d/jenkins
sudo chmod 0440 /etc/sudoers.d/jenkins

# 2. 复制 AWS 凭证给 jenkins 用户（否则 ECR 登录失败）
sudo mkdir -p /var/lib/jenkins/.aws
sudo cp ~/.aws/credentials /var/lib/jenkins/.aws/credentials
sudo cp ~/.aws/config /var/lib/jenkins/.aws/config 2>/dev/null || true
sudo chown -R jenkins:jenkins /var/lib/jenkins/.aws
sudo chmod 600 /var/lib/jenkins/.aws/credentials

# 3. 复制 SSH 密钥给 jenkins 用户（否则传镜像到私网机器失败）
sudo cp /home/ec2-user/aggpay_key.pem /var/lib/jenkins/aggpay_key.pem
sudo chown jenkins:jenkins /var/lib/jenkins/aggpay_key.pem
sudo chmod 600 /var/lib/jenkins/aggpay_key.pem

# 4. 验证三项权限均正常
sudo -u jenkins aws sts get-caller-identity
sudo -u jenkins ssh -i /var/lib/jenkins/aggpay_key.pem -o StrictHostKeyChecking=no ec2-user@10.0.1.38 "echo SSH OK"
sudo -u jenkins sudo docker ps
```

> **Jenkinsfile 中 KEY_PATH 必须填 `/var/lib/jenkins/aggpay_key.pem`**，不能用 `/home/ec2-user/aggpay_key.pem`（jenkins 用户无权访问其他用户的 home 目录）。

### GitHub 私有仓库

- Jenkins 凭据类型选 **Username with password**，Password 填 GitHub PAT（`repo` 权限），不要用 SSH Key 方式（需额外配置 known_hosts）
- PAT 生成路径：GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)
