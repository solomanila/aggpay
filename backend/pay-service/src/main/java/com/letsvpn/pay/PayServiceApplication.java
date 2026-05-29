package com.letsvpn.pay;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.letsvpn")
@EnableDiscoveryClient
@EnableScheduling
@EnableFeignClients(basePackages = {"com.letsvpn.pay.client"})
@MapperScan({"com.letsvpn.common.data.mapper","com.letsvpn.pay.mapper","com.letsvpn.pay.shopline.mapper"})
public class PayServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayServiceApplication.class, args);
    }
}
