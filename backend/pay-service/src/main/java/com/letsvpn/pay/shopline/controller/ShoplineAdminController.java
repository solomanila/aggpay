package com.letsvpn.pay.shopline.controller;

import com.letsvpn.pay.shopline.config.ShoplineConfig;
import com.letsvpn.pay.shopline.entity.ShoplineShopToken;
import com.letsvpn.pay.shopline.service.ShoplineOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pay/shopline/admin")
@RequiredArgsConstructor
@Slf4j
public class ShoplineAdminController {

    private final ShoplineConfig shoplineConfig;
    private final ShoplineOAuthService shoplineOAuthService;

    /**
     * App Bridge 嵌入页面。Shopline 后台通过 iframe 加载此页。
     * 访问方式：GET /shopline/admin?shop={shop_domain}
     */
    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> adminPage(@RequestParam(required = false) String shop) {
        String connected = "未知";
        if (shop != null) {
            String shopId = shoplineOAuthService.extractShopId(shop);
            ShoplineShopToken token = shoplineOAuthService.getByShopId(shopId);
            connected = token != null ? "已连接" : "未连接";
        }

        String html = buildAdminHtml(shoplineConfig.getAppKey(), shop, connected);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    private String buildAdminHtml(String appKey, String shop, String status) {
        String shopStr = shop != null ? shop : "";
        return "<!DOCTYPE html>\n" +
                "<html lang=\"zh-CN\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n" +
                "  <title>支付应用配置</title>\n" +
                "  <style>\n" +
                "    body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;margin:0;padding:24px;background:#f6f6f7;}\n" +
                "    .card{background:#fff;border-radius:8px;padding:24px;box-shadow:0 1px 3px rgba(0,0,0,.12);max-width:600px;margin:0 auto;}\n" +
                "    h1{font-size:20px;font-weight:600;color:#202223;margin:0 0 16px;}\n" +
                "    .badge{display:inline-block;padding:4px 12px;border-radius:12px;font-size:13px;font-weight:500;}\n" +
                "    .badge.ok{background:#e3f1df;color:#108043;}\n" +
                "    .badge.no{background:#fbeae5;color:#bf0711;}\n" +
                "    .row{display:flex;justify-content:space-between;align-items:center;padding:12px 0;border-bottom:1px solid #e1e3e5;}\n" +
                "    .row:last-child{border-bottom:none;}\n" +
                "    .label{color:#6d7175;font-size:14px;}\n" +
                "    .value{color:#202223;font-size:14px;font-weight:500;}\n" +
                "    .btn{display:inline-block;padding:8px 16px;background:#008060;color:#fff;border-radius:4px;text-decoration:none;font-size:14px;cursor:pointer;border:none;}\n" +
                "    .btn:hover{background:#006e52;}\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"card\">\n" +
                "    <h1>支付通道配置</h1>\n" +
                "    <div class=\"row\">\n" +
                "      <span class=\"label\">授权状态</span>\n" +
                "      <span class=\"badge " + ("已连接".equals(status) ? "ok" : "no") + "\">" + status + "</span>\n" +
                "    </div>\n" +
                "    <div class=\"row\">\n" +
                "      <span class=\"label\">店铺</span>\n" +
                "      <span class=\"value\">" + shopStr + "</span>\n" +
                "    </div>\n" +
                "    <div class=\"row\">\n" +
                "      <span class=\"label\">应用 Key</span>\n" +
                "      <span class=\"value\">" + appKey.substring(0, 8) + "****</span>\n" +
                "    </div>\n" +
                "    <div class=\"row\">\n" +
                "      <span class=\"label\">支付通道</span>\n" +
                "      <span class=\"badge ok\">已启用</span>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <script>\n" +
                "    // Shopline App Bridge 初始化\n" +
                "    // 参考文档: https://developer.myshopline.com/docsv2/ec20\n" +
                "    var urlParams = new URLSearchParams(window.location.search);\n" +
                "    var shop = urlParams.get('shop') || '" + shopStr + "';\n" +
                "    var host = urlParams.get('host');\n" +
                "    // 如需使用 App Bridge SDK，在此初始化:\n" +
                "    // ShoplineApp.createApp({ apiKey: '" + appKey + "', shopOrigin: shop });\n" +
                "    console.log('Shopline Admin Page loaded, shop:', shop);\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
