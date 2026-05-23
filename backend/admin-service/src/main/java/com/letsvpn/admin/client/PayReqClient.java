package com.letsvpn.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 专用于调用 pay-service 的 /pay/req 收款入口。
 * PayReqController 的 @RequestMapping("/pay") 没有 /api 前缀，
 * 所以不能复用 path="/api/pay" 的 PayServiceClient。
 */
@FeignClient(
        name = "pay-service",
        contextId = "payReqClient")
public interface PayReqClient {

    @GetMapping("/pay/req")
    String payReq(
            @RequestParam("fid") String fid,
            @RequestParam("uid") Long uid,
            @RequestParam("amount") String amount,
            @RequestParam("pf") String pf,
            @RequestParam("sign") String sign,
            @RequestParam("time") Long time,
            @RequestParam("cid") Long cid);
}
