package com.letsvpn.pay.shopline.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
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

    /**
     * 代理调用 Shopline bind.json，避免浏览器页面跳转取消 fetch 导致日志丢失。
     * 前端等本接口返回后再执行 window.location.href 跳转。
     */
    @PostMapping("/bind-notify")
    public ResponseEntity<Void> bindNotify(@RequestBody Map<String, Object> body) {
        String handle = String.valueOf(body.getOrDefault("handle", ""));
        String appKey = String.valueOf(body.getOrDefault("appKey", ""));
        String token  = String.valueOf(body.getOrDefault("token", ""));

        String url = "https://" + handle + ".myshopline.com/admin/openapi/v20260901/app/notify/bind.json";
        String reqBody = JSONUtil.toJsonStr(Map.of("handle", handle, "app_key", appKey));

        log.info("[Shopline bind-notify] 请求发起 url={} body={}", url, reqBody);

        try {
            HttpResponse resp = HttpRequest.post(url)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Authorization", "Bearer " + token)
                    .body(reqBody)
                    .timeout(10_000)
                    .execute();
            log.info("[Shopline bind-notify] 响应结果 status={} body={}", resp.getStatus(), resp.body());
        } catch (Exception e) {
            log.error("[Shopline bind-notify] 请求异常 url={} error={}", url, e.getMessage(), e);
        }

        return ResponseEntity.ok().build();
    }
}
