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
