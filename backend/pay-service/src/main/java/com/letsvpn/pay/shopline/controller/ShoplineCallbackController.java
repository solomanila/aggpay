package com.letsvpn.pay.shopline.controller;

import cn.hutool.core.util.StrUtil;
import com.letsvpn.pay.shopline.config.ShoplineConfig;
import com.letsvpn.pay.shopline.entity.ShoplineShopToken;
import com.letsvpn.pay.shopline.service.ShoplineOAuthService;
import com.letsvpn.pay.shopline.util.ShoplineSignUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/pay/shopline")
@RequiredArgsConstructor
@Slf4j
public class ShoplineCallbackController {

    private final ShoplineConfig shoplineConfig;
    private final ShoplineOAuthService shoplineOAuthService;

    /**
     * Shopline OAuth 回调（OAuth redirect_uri 指向此接口）。
     * 外部路径（经网关）：GET /api/pay/shopline/callback
     * 内部路径：GET /pay/shopline/callback
     *
     * Shopline 发送：?appkey=&code=&customField=&handle=&timestamp=&sign=
     *
     * 流程：
     * 1. 验证 HMAC-SHA256 签名
     * 2. POST /admin/oauth/token/create 换取 access_token
     * 3. 写库（INSERT 或 UPDATE shopline_shop_token）
     * 4. 跳转到 App Bridge 管理页面
     */
    @GetMapping("/callback")
    public void callback(@RequestParam Map<String, String> params,
                         HttpServletResponse response) throws IOException {
        String handle = params.get("handle");
        String code = params.get("code");
        String sign = params.get("sign");

        log.info("Shopline OAuth callback: handle={}", handle);

        if (StrUtil.isBlank(handle) || StrUtil.isBlank(code) || StrUtil.isBlank(sign)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameters");
            return;
        }

        if (!ShoplineSignUtil.verifyIncoming(params, sign, shoplineConfig.getAppSecret())) {
            log.warn("Shopline callback sign invalid: handle={}", handle);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
            return;
        }

        ShoplineShopToken token = shoplineOAuthService.createTokenFromCode(handle, code);
        if (token == null) {
            log.error("Shopline callback: failed to create token: handle={}", handle);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to obtain access token");
            return;
        }

        log.info("Shopline callback: token saved, redirecting to admin: handle={}", handle);
        response.sendRedirect("/pay/shopline/admin?shop=" + handle);
    }
}
