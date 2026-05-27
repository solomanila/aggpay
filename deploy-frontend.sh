#!/bin/bash
set -e
cd D:/ideaprojects/payadmin-ui
npm run build
scp -i D:/notes/aggpay_key.pem -r dist/* ec2-user@13.234.6.115:/tmp/frontend_dist/
ssh -i D:/notes/aggpay_key.pem ec2-user@13.234.6.115 \
  "sudo cp -r /tmp/frontend_dist/* /usr/share/nginx/html/ && sudo systemctl reload nginx"
echo "✅ 前端部署完成"
