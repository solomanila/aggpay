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
    def base   = "sudo docker run -d --name ${service} --restart always --network host -e TZ=Asia/Kolkata"
    def common = "-e SPRING_PROFILES_ACTIVE=prod -e NACOS_SERVER=10.0.1.130:8848 -e SPRING_CLOUD_NACOS_CONFIG_ENABLED=false -e MYSQL_HOST=10.0.1.176 -e MYSQL_USERNAME=root -e MYSQL_PASSWORD=Payadmin@2024 -e REDIS_HOST=10.0.1.130 -e REDIS_PASSWORD=Payadmin@2024"
    def proxy  = "-e 'JAVA_TOOL_OPTIONS=-Dhttps.proxyHost=10.0.1.30 -Dhttps.proxyPort=3128 -Dhttp.proxyHost=10.0.1.30 -Dhttp.proxyPort=3128 -Dhttp.nonProxyHosts=10.0.1.*|localhost|127.* -Dhttps.nonProxyHosts=10.0.1.*|localhost|127.*'"
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
