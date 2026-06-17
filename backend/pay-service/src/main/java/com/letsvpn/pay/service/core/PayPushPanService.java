package com.letsvpn.pay.service.core;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.letsvpn.pay.constant.AreaTypeConstants;
import com.letsvpn.pay.entity.OrderInfo;
import com.letsvpn.pay.entity.PayConfigChannel;
import com.letsvpn.pay.entity.PayPlatformInfo;
import com.letsvpn.pay.shopline.config.ShoplineConfig;
import com.letsvpn.pay.shopline.entity.ShoplineShopToken;
import com.letsvpn.pay.shopline.mapper.ShoplineShopTokenMapper;
import com.letsvpn.pay.shopline.service.ShoplineOAuthService;
import com.letsvpn.pay.shopline.util.ShoplineSignUtil;
import com.letsvpn.pay.mapper.MerchantInfoMapper;
import com.letsvpn.pay.mapper.PayConfigChannelMapper;
import com.letsvpn.pay.mapper.PayConfigInfoMapper;
import com.letsvpn.pay.mapper.PayPlatformInfoMapper;
import com.letsvpn.pay.mapper.core.ExtOrderPayPushPanMapper;
import com.letsvpn.pay.third.PayFormService;
import com.letsvpn.pay.util.PathOtherSystem;
import com.letsvpn.pay.util.PayConstant;
import com.letsvpn.pay.util.SignUtil;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

import com.letsvpn.pay.client.AdminBalanceClient;

import com.letsvpn.common.core.response.R;

@Service
@Slf4j
public class PayPushPanService extends BaseService {

//	@Autowired
//	TelegramService telegramService;
	@Autowired
	ExtOrderPayPushPanMapper extOrderPayPushPanMapper;
	@Autowired
	OrderBuildErrorService orderBuildErrorService;
	@Autowired
	PayPlatformInfoMapper extPayPlatformInfo;
	@Autowired
	PayConfigChannelMapper payConfigChannelMapper;
	@Autowired
	PayConfigInfoMapper payConfigInfoMapper;
	@Autowired
	MerchantInfoMapper merchantInfoMapper;
//	@Autowired
//	ApplicationContext applicationContext;
	@Autowired
	PayFormService payFormService;
	@Autowired
	RedisService redisService;

	@Autowired
	OrderInfoService orderInfoService;
	@Autowired
	PayPlatformInfoMapper payPlatformInfoMapper;

	@Autowired
	private AdminBalanceClient adminBalanceClient;

	@Autowired
	private ShoplineShopTokenMapper shoplineShopTokenMapper;

	@Autowired
	private ShoplineConfig shoplineConfig;

	@Autowired
	private ShoplineOAuthService shoplineOAuthService;

//	@Scheduled(fixedDelay = 3000, initialDelay = 10 * 1000)
//	public void payPushOrder() {
//		_extracted();
//	}
//
//	@Scheduled(fixedDelay = 4100, initialDelay = 16 * 1000)
//	public void payPushOrder2() {
//		_extracted();
//	}

	private void _extracted() {
		String orderId = redisService.rightPop(PayConstant.pay_success_list);
		while (orderId != null) {
			try {
				pushSuccessOrderInfo(Long.valueOf(orderId));
//				redisService.removeList(PayConstant.pay_success_list_bak, orderId);
			} catch (Exception e) {
				log.error("{}", e.getMessage(), e);
			}
			// log.info("orderId:{}", orderId);
			orderId = redisService.rightPop(PayConstant.pay_success_list);
//			orderId = redisService.rightPop(PayConstant.pay_success_list, PayConstant.pay_success_list_bak);
		}
	}

	/**
	 * 支付成功后处理：商户余额入账。
	 *
	 * <p>幂等设计（分布式重复投递安全）：
	 * <ol>
	 *   <li>先检查 noticeStatus == 100，已处理则直接返回。</li>
	 *   <li>余额入账成功后，将 noticeStatus 更新为 100。</li>
	 *   <li>入账失败则抛出异常，由 RocketMQ 重试机制处理。</li>
	 * </ol>
	 *
	 * <p>注意：不再使用 @Async。由 MQ 消费者线程池并发，ACK 在本方法返回后发出，
	 * 保证"处理成功才 ACK"的 at-least-once 语义。
	 */
	public void pushSuccessOrderInfo(Long id) {
		String reqid = UUID.randomUUID().toString().replaceAll("-", "");
		MDC.put(PayConstant.MDC_KEY_REQ_ID, reqid);

        OrderInfo info = orderInfoService.getOrderInfo(id);
        if (info == null) {
            log.warn("pushSuccessOrderInfo: order not found, id={}", id);
            return;
        }

        // 幂等：已处理过的订单直接跳过，防止重复入账
        if (Integer.valueOf(100).equals(info.getNoticeStatus())) {
            log.info("pushSuccessOrderInfo: already processed (noticeStatus=100), skip orderId={}",
                    info.getOrderId());
            return;
        }

        if (Integer.valueOf(1).equals(info.getStatus())) {
            // 余额入账失败时抛出异常，触发 RocketMQ 重试，不在此处吞掉错误
            R<Void> resp = adminBalanceClient.creditOrderIncome(
                    info.getPlatformId(), info.getRealAmount());
            if (resp == null || !R.isSuccess(resp.getCode())) {
                throw new RuntimeException(
                        "Balance credit failed for orderId=" + info.getOrderId()
                        + ", resp=" + (resp != null ? resp.getCode() : "null"));
            }
            log.info("merchant balance credited: orderId={}, platformId={}, amount={}",
                    info.getOrderId(), info.getPlatformId(), info.getRealAmount());
        }

        // 标记已处理：后续重复投递命中幂等检查，不会二次入账
        OrderInfo record = new OrderInfo();
        record.setId(id);
        record.setNoticeStatus(100);
        record.setNoticeTime(new Date());
        orderInfoService.updateByPrimaryKey(record);

        // 通知 Shopline 订单终态
        try {
            List<Integer> shoplinePlatformIds = payPlatformInfoMapper.selectShoplinePlatformIds();
            boolean isShopline = shoplinePlatformIds.contains(info.getPlatformId());
            if (isShopline) {
                String extend3 = info.getExtend3();
                if (StrUtil.isBlank(extend3)) {
                    log.warn("shopline notify: extend3 blank, skip. orderId={}", info.getOrderId());
                } else {
                    String notifyUrl = extend3;

                    PayPlatformInfo platformInfo = payPlatformInfoMapper.selectById(info.getPlatformId());
                    String currency = platformInfo != null ? AreaTypeConstants.currencyCode(platformInfo.getAreaType()) : "";

                    String accessToken = null;
                    if (platformInfo != null) {
                        ShoplineShopToken shopToken = shoplineShopTokenMapper.selectOne(
                                new QueryWrapper<ShoplineShopToken>().eq("shop_id", platformInfo.getPlatformNo()));
                        if (shopToken != null) {
                            accessToken = shopToken.getAccessToken();
							if (shoplineOAuthService.isExpired(shopToken)) {
								// 已安装但 token 过期，刷新
								log.info("Shopline install: token expired, refreshing: handle={}", shopToken.getShopHandle());
								ShoplineShopToken refreshed = shoplineOAuthService.refreshToken(shopToken.getShopHandle());
								accessToken = refreshed.getAccessToken();
							}
                        }
                    }

                    String amount = info.getReqAmount() != null
                            ? String.valueOf(info.getReqAmount().multiply(BigDecimal.valueOf(100)).longValue())
                            : "0";
                    String shoplineStatus = mapShoplineOrderStatus(info.getStatus());

                    JSONObject bodyJson = new JSONObject();
                    bodyJson.set("amount", amount);
                    bodyJson.set("channel_order_transaction_id", StrUtil.nullToEmpty(info.getOtherOrderId()));
                    bodyJson.set("currency", StrUtil.nullToEmpty(currency));
                    bodyJson.set("order_transaction_id", StrUtil.nullToEmpty(info.getFrontId()));
                    bodyJson.set("status", shoplineStatus);
                    String bodyStr = JSONUtil.toJsonStr(bodyJson);

					String timestamp = String.valueOf(System.currentTimeMillis());
					String sign = ShoplineSignUtil.buildOutgoingPost(bodyStr, shoplineConfig.getAppSecret(),timestamp);

//                    String signature = ShoplineSignUtil.signPayRequest(
//                            shoplineConfig.getResponsePrivateKey(), bodyJson);

                    log.info("shopline notify: orderId={} notifyUrl={} body={} sign={}",
                            info.getOrderId(), notifyUrl, bodyStr, sign);

                    String responseBody = HttpRequest.post(notifyUrl)
                            .header("Content-Type", "application/json; charset=utf-8")
                            .header("Authorization", "Bearer " + StrUtil.nullToEmpty(accessToken))
                            .header("sign", sign)
							.header("timestamp", timestamp)
                            .timeout(15000)
                            .body(bodyStr)
                            .execute()
                            .body();

                    log.info("shopline notify response: orderId={} response={}", info.getOrderId(), responseBody);
                }
            }
        } catch (Exception e) {
            log.warn("shopline notify failed: orderId={} err={}", info.getOrderId(), e.getMessage(), e);
        }

	}

    private String mapShoplineOrderStatus(Integer status) {
        if (status == null) return "PROCESSING";
        switch (status) {
            case 1: return "SUCCEEDED";
            case 2: return "FAILED";
            case 3: return "CANCELLED";
            default: return "PROCESSING";
        }
    }

	void _pushSuccessOrderInfo(Long id, String reqid) {
		OrderInfo info = orderInfoService.getOrderInfo(id);
		log.info("id:{},OrderInfo:{}", id, JSONUtil.toJsonStr(info));
		if (info == null)
			return;
		// 1成功，8退款成功

		// 6 待退款 refund_init 7 退款中 refund_requested 8 退款成功 refund_success 9 退款失败
		// refund_fail
		Integer status = info.getStatus();
		if (",1,6,7,8,9,".indexOf(status.toString()) == -1) {
			log.info("OrderInfo:{},status != 1 {} , {}", status, id);
			return;
		}
		if (status == 2) {
			log.info("OrderInfo:{},statusguadan ==2 {} , {}", status, id);
			return;
		}
		if (status == 1 && info.getNoticeStatus() == 100) {
			log.info("OrderInfo:{},status != 1 {} , {}", status, id);
			return;
		}
		if (status == 6 && info.getNoticeStatus() == 6) {
			log.info("OrderInfo:{},status != 6 {} , {}", status, id);
			return;
		}
		if (status == 7 && info.getNoticeStatus() == 7) {
			log.info("OrderInfo:{},status != 7 {} , {}", status, id);
			return;
		}
		if (status == 8 && info.getNoticeStatus() == 8) {
			log.info("OrderInfo:{},status != 8 {} , {}", status, id);
			return;
		}
		if (status == 9 && info.getNoticeStatus() == 9) {
			log.info("OrderInfo:{},status != 9 {} , {}", status, id);
			return;
		}

		Long channelId = info.getPayConfigChannelId();
		PayConfigChannel channel = payConfigChannelMapper.selectById(channelId);
		if (channel == null) {
			return;
		}
		Integer shareId = channel.getShareId();

		Integer platformId = info.getPlatformId();
		QueryWrapper<PayPlatformInfo> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("platform_id", platformId);

		PayPlatformInfo platformInfo = payPlatformInfoMapper.selectOne(queryWrapper);
		log.info("platformInfo:{}", JSONUtil.toJsonStr(platformInfo));
		if (platformInfo == null)
			return;
		String skey = platformInfo.getSecretKey();
		String platformNo = platformInfo.getPlatformNo();
		String orderId = info.getOrderId();
		Map<String, String> map = new HashMap<>();
		map.put("id", info.getId() + "");
		map.put("shareId", shareId + "");
		map.put("reqid", reqid);
		map.put("projectSign", platformNo);
		map.put("randomId", System.currentTimeMillis() + "");
		map.put("orderId", orderId);
		map.put("createIp", info.getCreateIp());
		map.put("realAmount", info.getRealAmount() + "");
		map.put("status", info.getStatus() + "");
		map.put("reqAmount", info.getReqAmount() + "");
		map.put("userId", info.getUserId() + "");
		if (StringUtil.isNotEmpty(info.getUpi())) {
			map.put("upi", info.getUpi());
		}
		if (StringUtil.isNotEmpty(info.getExtend1())) {
			map.put("extend1", info.getExtend1());
		}
		if (StringUtil.isNotEmpty(info.getExtend2())) {
			map.put("extend2", info.getExtend2());
		}
		if (StringUtil.isNotEmpty(info.getExtend3())) {
			map.put("extend3", info.getExtend3());
		}
		map.put("frontId", info.getFrontId());
		map.put("payConfigChannelId", info.getPayConfigChannelId() + "");
		map.put("payConfigId", info.getPayConfigId() + "");
		map.put("platformId", info.getPlatformId() + "");
		map.put("payTime", DateUtil.formatDateTime(info.getPayTime()));
		map.put("createTime", DateUtil.formatDateTime(info.getCreateTime()));
		map.put("otherOrderId", info.getOtherOrderId());
		String sign = SignUtil.signMap(map, skey);
		map.put("sign", sign);
		String domain = platformInfo.getDomain();
		String url = "";
		if (StrUtil.isBlank(domain)) {
			url = platformInfo.getCallbackUrl();
		} else {
			url = domain + PathOtherSystem.pushOnLinePayOrder;
		}
		log.info("domain:{}, url:{}, map:{}", domain, url, map);
		String result2 = null;

		long tt = System.currentTimeMillis();
		result2 = postRequest(map, url, info);

		log.info("postReq {} , {} , {} , {} , {}", (System.currentTimeMillis() - tt), url, orderId, platformNo,
				result2);
		// {"msg":"ok","code":0,"data":{"gameId":517074,"onLineId":1920950,"orderId":"200930220744jjpay164m2ak","remark":"0.96->100.96","status":"ok"}}
		// data
		if (JSONUtil.isJsonObj(result2)) {
			updateOrderInfo(info, platformInfo.getTitle(), orderId, JSONUtil.parseObj(result2));

		} else {
			orderBuildErrorService.add(info.getPlatformId(), info.getPayConfigId(), info.getPayConfigChannelId(),
					"上分失败", HtmlUtil.escape(result2), "" + orderId, this.getClass().getSimpleName());
			extOrderPayPushPanMapper.addOrderPayPushPan(id);
		}


	}

	private String postRequest(Map<String, String> map, String url, OrderInfo info) {
		HttpRequest hr = HttpRequest.post(url).body(new JSONObject(map).toString()).timeout(10 * 1000);// 超时，毫秒
		for (int i = 0; i < 5; i++) {
			try {
				return hr.executeAsync().body();
			} catch (Exception e) {
//				if (e instanceof UnknownHostException) {
//					log.error("UnknownHostException {},{},{}", url, map, e.getMessage(), e);
//				}
//				if (e instanceof HttpException) {
//					log.error("HttpException {},{},{}", url, map, e.getMessage(), e);
//				}
				log.error("Exception {}, {}, {}, {}", i, url, map, e.getMessage(), e);
				orderBuildErrorService.add(info.getPlatformId(), info.getPayConfigId(), info.getPayConfigChannelId(),
						info.getOrderId(), map.toString(), i + "->" + e.getMessage(), this.getClass().getSimpleName());
				// throw e;
			}
		}
		extOrderPayPushPanMapper.addOrderPayPushPan(info.getId());
		throw new RuntimeException("执行了5次还是没有成功!" + info.getOrderId());
	}

	private void updateOrderInfo(OrderInfo info2, String title, String orderId1, JSONObject parseObj) {
		if (parseObj.getInt("code") == 0) {
			Long id = info2.getId();
			JSONObject data = parseObj.getJSONObject("data");
			String orderId = MapUtil.getStr(data, "orderId", "");
			validate(orderId == null, "orderId is null " + orderId, 404);
			String status = MapUtil.getStr(data, "status", "");
			OrderInfo info = orderInfoService.getOrderInfo(orderId);
			validate(info == null, "不存在!" + orderId, 404);
			// validate(info.getNoticeStatus() == 1, "已经处理!" + orderId, 404);
			if ("ok".equals(status)) {
				String remark = MapUtil.getStr(data, "remark", "");
				// Integer gameId = data.getInt("gameId");

//				gameId

				OrderInfo record = new OrderInfo();
				record.setId(id);
				if (info.getGameId() == null || info.getGameId() <= 0) {
					Integer gameId = MapUtil.getInt(data, "gameId", 0);
					record.setGameId(gameId);
				}

				// record.setGameId(gameId);
				record.setNoticeStatus(100);
				record.setCreateStatus(100);
				record.setNoticeTime(new Date());
				record.setRemark(remark);

				Long onLineId = MapUtil.getLong(data, "onLineId", 0l);
				record.setOnLineId(onLineId);

//				OrderInfoExample example = new OrderInfoExample();
//				example.createCriteria().andOrderIdEqualTo(orderId);

				int a = orderInfoService.updateByPrimaryKey(record);

				if (a > 0) {
					//updatePayConfigLimit(info);
				}

				log.info("a:{}", a);
			} else if ("6".equals(status) || "7".equals(status) || "8".equals(status) || "9".equals(status)) {
				String remark = MapUtil.getStr(data, "remark", "");

				OrderInfo record = new OrderInfo();
				record.setId(id);

				record.setNoticeStatus(Integer.valueOf(status));
				record.setNoticeTime(new Date());

				if (StrUtil.isNotBlank(remark)) {
					record.setRemark(remark);
				}

				if (info2.getCreateStatus() == 0) {
					record.setCreateStatus(100);
				}
				orderInfoService.updateByPrimaryKey(record);

			} else if ("offline".equals(status)) {
				String remark = MapUtil.getStr(data, "remark", "");

				OrderInfo record = new OrderInfo();
				record.setId(id);

				record.setNoticeStatus(100);
				record.setCreateStatus(100);
				record.setNoticeTime(new Date());
				record.setRemark(remark);

				orderInfoService.updateByPrimaryKey(record);

			} else {
				String text = "上分失败!!" + orderId + title + data.toString();
//				telegramService.sendMsg(RedisConstant.redis_root_telegram_error, text);

				orderBuildErrorService.add(info.getPlatformId(), info.getPayConfigId(), info.getPayConfigChannelId(),
						"", text, "", this.getClass().getSimpleName());
				extOrderPayPushPanMapper.addOrderPayPushPan(info2.getId());
			}

		} else if (parseObj.getInt("code") == 3021) {
			OrderInfo record = new OrderInfo();
			if (info2.getCreateStatus() != 100) {
				record.setCreateStatus(100);
			}
			if (info2.getNoticeStatus() != 100) {
				record.setNoticeStatus(100);
			}
			record.setId(info2.getId());
			orderInfoService.updateByPrimaryKey(record);
		} else {
			String text = "上分失败!" + title + "订单" + orderId1 + "返回:" + parseObj.toString();
//			telegramService.sendMsg(RedisConstant.redis_root_telegram_error, text);
			orderBuildErrorService.add(info2.getPlatformId(), info2.getPayConfigId(), info2.getPayConfigChannelId(), "",
					text, orderId1, this.getClass().getSimpleName());

		}
	}



	public List<Long> manualOrderPayPushPan(int type) {
		List<Long> list = extOrderPayPushPanMapper.groupOrderPayPushPan();
		if (type > 0) {
			for (Long id : list) {
				Long i = redisService.leftPush(PayConstant.pay_success_list, id.toString());
				log.info("forceUpScore:{}, id:{}, a:{}, i:{}", id, i);
				extOrderPayPushPanMapper.delOrderPayPushPan(id);
			}
		}
		return list;
	}

}
