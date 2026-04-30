package com.letsvpn.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.letsvpn")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.letsvpn.admin.client")
@EnableScheduling
@MapperScan({"com.letsvpn.admin.mapper","com.letsvpn.common.data.mapper"})
public class AdminServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
