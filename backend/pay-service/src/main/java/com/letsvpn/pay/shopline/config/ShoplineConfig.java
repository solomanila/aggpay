package com.letsvpn.pay.shopline.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class ShoplineConfig {

    @Value("${shopline.app-key}")
    private String appKey;

    @Value("${shopline.app-secret}")
    private String appSecret;

    @Value("${shopline.public-key}")
    private String publicKey;

    @Value("${shopline.platform-no}")
    private String platformNo;

    @Value("${shopline.api-base:https://openapi.myshopline.com}")
    private String apiBase;

    @Value("${shopline.api-version:2023-01}")
    private String apiVersion;

    @Value("${shopline.redirect-uri}")
    private String redirectUri;

    @Value("${shopline.scopes:write_discounts,write_store_information,read_store_information,write_payment_gateways,read_script_tags,write_shop_policy,write_price_rules,write_fulfillment_service,read_bulkoperation,write_inventory,read_orders,write_content,read_marketing_event,read_inventory,write_orders,read_discounts,read_products,read_content,read_returns,read_fulfillment_service,write_products,read_location,read_shipping,read_translation,read_selling_plan_group,read_themes,write_subscription_contracts,write_bulkoperation,write_translation,read_customers,read_payment,write_checkouts,read_shop_policy,write_customers,read_markets,read_product_listings,write_files,read_store_metrics,read_gift_card,write_script_tags,write_product_listings,write_return,write_page,read_subscription_contracts,write_selling_plan_group,read_assigned_fulfillment_orders,write_shipping,read_page,write_themes,read_price_rules,write_gift_card,write_markets,write_assigned_fulfillment_orders,read_files,read_draft_orders,write_marketing_event,write_draft_orders}")
    private String scopes;
}
