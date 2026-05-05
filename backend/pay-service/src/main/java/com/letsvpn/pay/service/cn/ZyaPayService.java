package com.letsvpn.pay.service.cn;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.letsvpn.pay.dto.IPayNotifyHandle;
import com.letsvpn.pay.dto.IPayThirdRequest;
import com.letsvpn.pay.entity.MerchantInfo;
import com.letsvpn.pay.entity.PayConfigChannel;
import com.letsvpn.pay.entity.PayConfigInfo;
import com.letsvpn.pay.entity.PayConfigParameter;
import com.letsvpn.pay.exception.WanliException;
import com.letsvpn.pay.third.BaseThirdService;
import com.letsvpn.pay.third.util.ThirdUtil;
import com.letsvpn.pay.util.PayCallMethod;
import com.letsvpn.pay.util.PayConfigChannelExtractType;
import com.letsvpn.pay.util.PayHttpType;
import com.letsvpn.pay.vo.PayResultData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.*;

/**
 * A8支付
 *
 * @author Daniel
 */
@Service
@Slf4j
public class ZyaPayService extends BaseThirdService implements IPayThirdRequest, IPayNotifyHandle {


    @SuppressWarnings("deprecation")
    @Override
    public PayResultData executeReq(MerchantInfo merchantInfo, PayConfigInfo payConfigInfo, PayConfigChannel channel,
                                    Map<String, String> param, List<PayConfigParameter> payConfigParameters) {
        if (payConfigInfo == null)
            return null;

        log.debug("-----------------------");
        StringBuffer logText = new StringBuffer();
        logText.append(JSONUtil.toJsonStr(merchantInfo)).append("\n");
        PayCallMethod callmethod = PayCallMethod.valueOf(payConfigInfo.getCallMethod());
        PayResultData result = new PayResultData(callmethod);

        Map<String, String> formData = new HashMap<String, String>();
        ThirdUtil.extracted2(param, payConfigParameters, formData, logText);
        log.debug("---------------------1-");
        for (Map.Entry<String, String> payReqParam : formData.entrySet()) {
            log.debug("{}", payReqParam);
        }
        log.debug("--------------------2--");
        String reqLink = payConfigInfo.getUrl();

        Map<String, Object> formData1 = new HashMap<String, Object>();
        for (Map.Entry<String, String> payReqParam : formData.entrySet()) {
            formData1.put(payReqParam.getKey(), payReqParam.getValue());
        }

        long now = System.currentTimeMillis();
        String resultText = null;
        try {
            log.debug("reqLink:{}", reqLink);
            if (StrUtil.isEmpty(payConfigInfo.getHttpType())) {
                payConfigInfo.setHttpType(PayHttpType.form_data.name());
            }
            PayHttpType httpType = PayHttpType.valueOf(payConfigInfo.getHttpType());

            HttpRequest hr = HttpRequest.post(reqLink).timeout(ThirdUtil.HTTP_REQUEST_TIMEOUT);// 超时，毫秒

//            hr = hr.form(formData1);
//            resultText = hr.executeAsync().body();


            String jsonBody = JSONUtil.toJsonStr(formData1);
            resultText = HttpRequest.post(reqLink)
                    .timeout(ThirdUtil.HTTP_REQUEST_TIMEOUT)
                    .header("keyId", merchantInfo.getAppId())
                    .header("sign", MapUtil.getStr(formData1,"sign",null))
                    .header("Content-Type", "application/json")
                    .body(jsonBody)
                    .execute()
                    .body();



            log.debug("resultText:{}", resultText);
            String etype = channel.getExtractType();
            if (StrUtil.isEmpty(etype)) {
                etype = PayConfigChannelExtractType.html.name();
            }
            result.setMethod(PayCallMethod.valueOf(payConfigInfo.getCallMethod()));
            result.setHtml(buildPaymentHtml(resultText, param));

//            result.setMethod(PayCallMethod.valueOf(payConfigInfo.getCallMethod()));
//            if (etype.compareTo(String.valueOf(PayConfigChannelExtractType.json)) == 0) {
//                JSONObject json = JSONUtil.parseObj(resultText);
//                String redirect = ThirdUtil.getJsonField(channel, json);
//                if (StrUtil.isEmpty(redirect)) {
//                    throw new WanliException(resultText);
//                }
//                result.setLink(redirect);
//            }



        } catch (WanliException e) {
            log.error(e.getMessage(), e);
           add(merchantInfo.getPlatformId(), payConfigInfo.getId(), channel.getId(),
                    channel.getTitle(), e.getMessage(), resultText, this.getClass().getSimpleName());
            if (payConfigInfo.getTest() == 1) {
                throw e;
            } else {
                long reqTime = System.currentTimeMillis() - now;
                extracted(merchantInfo, payConfigInfo, channel, reqLink, formData1, resultText, reqTime, e.getMessage(),
                        logText, 1);
                throw new WanliException(5011,"该支付异常1,请换其他支付");
            }
        } catch (Exception e) {
            add(merchantInfo.getPlatformId(), payConfigInfo.getId(), channel.getId(),
                    channel.getTitle(), e.getMessage(), resultText, this.getClass().getSimpleName());
            long reqTime = System.currentTimeMillis() - now;
            extracted(merchantInfo, payConfigInfo, channel, reqLink, formData1, resultText, reqTime, e.getMessage(),
                    logText, 1);
            log.error(e.getMessage(), e);
            throw new WanliException(5012,"该支付异常2,请换其他支付");
        }
        long reqTime = System.currentTimeMillis() - now;
        extracted(merchantInfo, payConfigInfo, channel, reqLink, formData1, resultText, reqTime, "", logText, 0);

        return result;
    }

    @Override
    public void paramExtraction(Map<String, String> paramMap, MerchantInfo merchantInfo) {

    }

    @Override
    public boolean signCheck(Map<String, String> paramsMap, List<PayConfigParameter> payConfigParameters,
                             StringBuffer logText) {
        Map<String, String> formData = new HashMap<String, String>();
        ThirdUtil.extracted2(paramsMap, payConfigParameters, formData, logText);
        logText.append(formData).append("\n\n");

        for (PayConfigParameter payConfigParameter : payConfigParameters) {
            log.debug("payConfigParameters{}", JSONUtil.toJsonStr(payConfigParameter));
            String payName = payConfigParameter.getPayName();
            String paramValue = paramsMap.get(payName);
            log.debug("payName:{}, paramValue:{}", payName, paramValue);
            log.debug("formData:{}", formData);
            logText.append(payName).append(":").append(paramValue).append("\n\n");

            if (!MapUtil.getStr(formData, payName, "").equalsIgnoreCase(paramValue)) {
                return false;
            }
        }
        log.debug("paramsMap{}", paramsMap);
        return true;

    }

    private String buildPaymentHtml(String resultText, Map<String, String> param) {
        String payUrl = resultText;
        try {
            JSONObject json = JSONUtil.parseObj(resultText);
            for (String field : Arrays.asList("payUrl", "url", "pay_url", "redirect_url", "redirectUrl", "link", "cashierUrl", "data")) {
                String v = json.getStr(field);
                if (StrUtil.isNotEmpty(v)) {
                    payUrl = v;
                    break;
                }
            }
        } catch (Exception ignored) {}

        String amount = MapUtil.getStr(param, "payAmount", "");

        try {
            String template = ResourceUtil.readUtf8Str("static/zya-pay.html");
            return template
                    .replace("{{AMOUNT}}", amount)
                    .replace("{{PAYMENT_URL}}", payUrl);
        } catch (Exception e) {
            log.error("Failed to load zya-pay.html template", e);
            return payUrl;
        }
    }

    public static void main(String[] args) {
        Map<String,String> map = new HashMap<>();
        map.put("title","pc");
        map.put("currency","INR");
        map.put("outTradeNo","");
        map.put("payAmount","100");
        map.put("callbackUrl","null/api/pay/notify3Headers/10/4");
        map.put("phone","+916431086668");
        map.put("firstname","micheal");
        map.put("lastname","jackson");
        map.put("uid","1777970140339");
        map.put("clientIp","127.0.0.1");
        map.put("email","jackson@gmail.com");



        // Sort the parameters in dictionary order by parameter name
        TreeMap<String, Object> treeMap = new TreeMap<>(map);
        // Concatenate the parameters, following the rule of a=1&b=2
        ArrayList<String> strList = new ArrayList<>();
        for (String key : treeMap.keySet()) {
            Object value = treeMap.get(key);
            if (value != null) {
                // Fields with null values will not participate in the signature
                strList.add(key + "=" + value);
            }
        }
        // Append appKey=xxx at the end of the concatenated string
        strList.add("appKey=");
        String str = String.join("&", strList);
        System.out.println(str);
        // Calculate the MD5 value in lowercase
         DigestUtils.md5DigestAsHex(str.getBytes());
    }
}