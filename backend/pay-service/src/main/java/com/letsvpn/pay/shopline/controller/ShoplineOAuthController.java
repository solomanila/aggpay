package com.letsvpn.pay.shopline.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
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
@RequestMapping("/pay/shopline/oauth")
@RequiredArgsConstructor
@Slf4j
public class ShoplineOAuthController {

    private final ShoplineConfig shoplineConfig;
    private final ShoplineOAuthService shoplineOAuthService;

    /**
     * Shopline 安装入口。
     * Shopline 后台点击安装时调用：GET /pay/shopline/oauth/install?appkey=&handle=&lang=&timestamp=&sign=
     *
     * 流程：
     * 1. 验证 HMAC-SHA256 签名
     * 2. 查询 shopline_shop_token WHERE shop_handle = handle
     *    - 未安装 → 跳转 Shopline 授权页面
     *    - 已安装但 token 已过期 → 调用 token/refresh 续期，结果写库
     *    - 已安装且有效 → 跳转管理页面
     */
    @GetMapping("/install")
    public void install(@RequestParam Map<String, String> params,
                        HttpServletResponse response) throws IOException {
        String handle = params.get("handle");
        String sign = params.get("sign");

        log.info("Shopline install: handle={}", handle);

        if (StrUtil.isBlank(handle) || StrUtil.isBlank(sign)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameters");
            return;
        }

        if (!ShoplineSignUtil.verifyIncoming(params, sign, shoplineConfig.getAppSecret())) {
            log.warn("Shopline install sign invalid: handle={}", handle);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
            return;
        }

        ShoplineShopToken token = shoplineOAuthService.getByHandle(handle);

        if (token == null) {
            // 未安装，跳转授权页面
            String encodedRedirectUri = URLUtil.encode(shoplineConfig.getRedirectUri());
            String authUrl = "https://" + handle + ".myshopline.com/admin/oauth-web/#/oauth/authorize"
                    + "?appKey=" + shoplineConfig.getAppKey()
                    + "&responseType=code"
                    + "&scope=" + URLUtil.encode(shoplineConfig.getScopes())
                    + "&redirectUri=" + encodedRedirectUri;
            log.info("Shopline install: not installed, redirecting to auth: handle={}", handle);
            response.sendRedirect(authUrl);
            return;
        }

        if (shoplineOAuthService.isExpired(token)) {
            // 已安装但 token 过期，刷新
            log.info("Shopline install: token expired, refreshing: handle={}", handle);
            ShoplineShopToken refreshed = shoplineOAuthService.refreshToken(handle);
            if (refreshed == null) {
                // 刷新失败，重新走授权流程
                String encodedRedirectUri = URLUtil.encode(shoplineConfig.getRedirectUri());
                String authUrl = "https://" + handle + ".myshopline.com/admin/oauth-web/#/oauth/authorize"
                        + "?appKey=" + shoplineConfig.getAppKey()
                        + "&responseType=code"
                        + "&scope=" + URLUtil.encode(shoplineConfig.getScopes())
                        + "&redirectUri=" + encodedRedirectUri;
                log.warn("Shopline install: refresh failed, re-authorizing: handle={}", handle);
                response.sendRedirect(authUrl);
                return;
            }
        }

        // 已安装且有效，无需任何操作
        log.info("Shopline install: already installed and valid, handle={}", handle);
    }
}
