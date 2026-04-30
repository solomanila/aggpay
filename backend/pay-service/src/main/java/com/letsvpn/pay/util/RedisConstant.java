package com.letsvpn.pay.util;

public class RedisConstant {

	public static final String redis_root = "payproject";
	public static final String redis_root_telegram = redis_root + ":telegram";
	public static final String redis_root_telegram_error = redis_root_telegram + ":error";
	public static final String redis_root_telegram_test = redis_root_telegram + ":test";

	public static final String SYSTEM_NAME = "platform_system:";

	/**登录人信息key前缀*/
	public static final String LOGIN_KEY = RedisConstant.SYSTEM_NAME + "LOGIN:";

	/**pbank补单锁消息*/
	public final static String PUSH_LOCK_MESSAGE = "pbankUtrPush in progress!";

	public static String orderlock(String orderId) {
		return redis_root + ":pay_notify:" + orderId;
	}

	public static String utrlock(String utr) {
		return redis_root + ":pay_query:" + utr;
	}

	public static String getKey(String key) {
		return RedisConstant.SYSTEM_NAME + key;
	}

	public static String getLoginKey(String userAccount) {
		return RedisConstant.LOGIN_KEY + userAccount;
	}

	// 动态配置 Key（由 admin-service 写入）
	public static String rlConfigChannelTps(int payConfigId)  { return redis_root + ":rl:cfg:channel:" + payConfigId + ":tps"; }
	public static String rlConfigMerchantTps(String appId)    { return redis_root + ":rl:cfg:merchant:" + appId + ":tps"; }
	public static String rlConfigGlobalTps()                  { return redis_root + ":rl:cfg:global:tps"; }
	public static String rlConfigIpLimit()                    { return redis_root + ":rl:cfg:ip:limit"; }
	public static String rlConfigUserLimit()                  { return redis_root + ":rl:cfg:user:limit"; }

	// 计数器 Key
	public static String rlIp(String ip)                      { return redis_root + ":rl:ip:" + ip; }
	public static String rlUser(long uid)                     { return redis_root + ":rl:user:" + uid; }
	public static String rlGlobal()                           { return redis_root + ":rl:global"; }
	public static String rlChannel(int payConfigId)           { return redis_root + ":rl:channel:" + payConfigId; }
	public static String rlMerchant(String appId)             { return redis_root + ":rl:merchant:" + appId; }
	public static String rlIdempotency(int pid, String fid)   { return redis_root + ":rl:idempotency:" + pid + ":" + fid; }
}
