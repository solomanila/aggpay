package com.letsvpn.pay.shopline.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/pay/shopline")
@Slf4j
public class ShoplineBindLogController {

    @PostMapping("/bind-log")
    public ResponseEntity<Void> bindLog(@RequestBody Map<String, Object> body) {
        String type    = String.valueOf(body.getOrDefault("type", ""));
        String url     = String.valueOf(body.getOrDefault("url", ""));
        String reqBody = String.valueOf(body.getOrDefault("reqBody", ""));
        String status  = String.valueOf(body.getOrDefault("status", ""));
        String resp    = String.valueOf(body.getOrDefault("resp", ""));
        String error   = String.valueOf(body.getOrDefault("error", ""));

        if ("request".equals(type)) {
            log.info("[Shopline bind] 请求发起 url={} body={}", url, reqBody);
        } else if ("response".equals(type)) {
            log.info("[Shopline bind] 响应结果 status={} body={}", status, resp);
        } else if ("error".equals(type)) {
            log.error("[Shopline bind] 请求异常 url={} error={}", url, error);
        } else {
            log.info("[Shopline bind] {}", body);
        }
        return ResponseEntity.ok().build();
    }
}
