package com.letsvpn.pay.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.letsvpn.common.core.dto.MerchantBalanceDTO;
import com.letsvpn.common.core.dto.PlatformAccountDTO;
import com.letsvpn.common.core.response.R;
import com.letsvpn.pay.client.AdminBalanceClient;
import com.letsvpn.pay.client.AdminUserAuthClient;
import com.letsvpn.pay.entity.OrderInfo;
import com.letsvpn.pay.mapper.OrderInfoMapper;
import com.letsvpn.pay.mapper.ext.ExtBankPayUtrMapper;
import com.letsvpn.pay.dto.MerchantRateDTO;
import com.letsvpn.pay.service.core.DashboardMetricsService;
import com.letsvpn.pay.service.core.ManualCallbackService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.SendResponse;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@ConditionalOnProperty(name = "telegram-bot.enabled", havingValue = "true")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TelegramBotService implements UpdatesListener {

    @Value("${telegram-bot.token}")
    private String telegramBotToken;

    private TelegramBot bot;
    private final RestTemplate restTemplate = new RestTemplate();

    private final OrderInfoMapper       orderInfoMapper;
    private final ExtBankPayUtrMapper   extBankPayUtrMapper;
    private final ManualCallbackService manualCallbackService;
    private final DashboardMetricsService dashboardMetricsService;
    private final AdminBalanceClient    adminBalanceClient;
    private final AdminUserAuthClient   adminUserAuthClient;

    private static final String HELP_TEXT =
            "h=help | i=payin | o=payout | q=check | u=UTR | n=Notify | b=balance\n" +
            "-------------\n" +
            "/h 完整查询代码获取\n" +
            "/b 查询代付账户余额\n" +
            "/i q SIXXX(代收订单号) = 查询订单状态\n" +
            "/i u 12038152192(UTR) = 查询UTR记录\n" +
            "/o q SPXXX(代付订单号) = 查询订单状态\n" +
            "/n i SIXXX(代收订单号) = 手动回调\n" +
            "/n o SPXXX(代付订单号) = 手动回调\n" +
            "/n u SIXXX(代收订单号) UTR = UTR补单\n" +
            "/a q 今日收款户\n" +
            "/doc 查看api文档\n" +
            "/r [分钟] 商户代收成功率(默认5分钟)";

    private static final String DOC_TEXT =
            "📄 API 文档\n" +
            "English: https://bedecked-neighbor-f5b.notion.site/ebd//25c605a959a9803788b3ce87dcecdfd8\n" +
            "中文: https://bedecked-neighbor-f5b.notion.site/ebd//25c605a959a9807ca08fddd712a7de84";

    @PostConstruct
    public void init() {
        log.info(">>>>>> 正在准备启动 Telegram Bot...");
        this.run();
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            if (update.message() != null && update.message().text() != null) {
                String text   = update.message().text().trim();
                long   chatId = update.message().chat().id();
                log.info(">>> 收到消息: {} 来自 ChatID: {}", text, chatId);
                routeCommand(chatId, text);
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    // ── 路由 ─────────────────────────────────────────────────────────────

    private void routeCommand(long chatId, String text) {
        String[] parts = text.split("\\s+");
        String   cmd   = parts[0].toLowerCase();
        try {
            switch (cmd) {
                case "/h":   sendMessage((byte) 0, chatId, HELP_TEXT); break;
                case "/doc": sendMessage((byte) 0, chatId, DOC_TEXT);  break;
                case "/b":   handleQueryCommand(chatId);  break;
                case "/c":   handleQueryCommandC(chatId); break;
                case "/i":   routePayinCommand(chatId, parts);   break;
                case "/o":   routePayoutCommand(chatId, parts);  break;
                case "/n":   routeNotifyCommand(chatId, parts);  break;
                case "/a":   routeAccountCommand(chatId, parts); break;
                case "/r":   handleSuccessRate(chatId, parts);   break;
                default:
                    sendMessage((byte) 0, chatId, "❓ 未知指令，发送 /h 查看帮助");
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            sendMessage((byte) 0, chatId, "❌ 参数不足，发送 /h 查看帮助");
        } catch (Exception e) {
            log.error("指令处理异常: {}", text, e);
            sendMessage((byte) 0, chatId, "❌ 处理异常: " + e.getMessage());
        }
    }

    private void routePayinCommand(long chatId, String[] parts) {
        if (parts.length < 3) { sendMessage((byte) 0, chatId, "❌ 用法: /i q <订单号>  或  /i u <UTR>"); return; }
        String sub = parts[1].toLowerCase();
        if ("q".equals(sub))      handleOrderQuery(chatId, parts[2], "代收");
        else if ("u".equals(sub)) handleUtrQuery(chatId, parts[2]);
        else sendMessage((byte) 0, chatId, "❌ 未知子命令，用法: /i q <订单号>  或  /i u <UTR>");
    }

    private void routePayoutCommand(long chatId, String[] parts) {
        if (parts.length < 3) { sendMessage((byte) 0, chatId, "❌ 用法: /o q <订单号>"); return; }
        String sub = parts[1].toLowerCase();
        if ("q".equals(sub)) handleOrderQuery(chatId, parts[2], "代付");
        else sendMessage((byte) 0, chatId, "❌ 未知子命令，用法: /o q <订单号>");
    }

    private void routeNotifyCommand(long chatId, String[] parts) {
        if (parts.length < 3) { sendMessage((byte) 0, chatId, "❌ 用法: /n i <订单号>  /n o <订单号>  /n u <订单号> <UTR>"); return; }
        String sub = parts[1].toLowerCase();
        if ("i".equals(sub) || "o".equals(sub)) {
            handleManualCallback(chatId, parts[2]);
        } else if ("u".equals(sub)) {
            if (parts.length < 4) { sendMessage((byte) 0, chatId, "❌ 用法: /n u <订单号> <UTR>"); return; }
            handleUtrSupply(chatId, parts[2], parts[3]);
        } else {
            sendMessage((byte) 0, chatId, "❌ 未知子命令，用法: /n i|o <订单号>  或  /n u <订单号> <UTR>");
        }
    }

    private void routeAccountCommand(long chatId, String[] parts) {
        if (parts.length < 2) { sendMessage((byte) 0, chatId, "❌ 用法: /a q"); return; }
        String sub = parts[1].toLowerCase();
        if ("q".equals(sub)) handleTodayAccounts(chatId);
        else sendMessage((byte) 0, chatId, "❌ 未知子命令，用法: /a q");
    }

    // ── 指令处理 ─────────────────────────────────────────────────────────

    private void handleOrderQuery(long chatId, String orderId, String label) {
        OrderInfo order = orderInfoMapper.selectOne(
                Wrappers.<OrderInfo>lambdaQuery().eq(OrderInfo::getOrderId, orderId));
        if (order == null) {
            sendMessage((byte) 0, chatId, "❌ 订单不存在: " + orderId);
            return;
        }
        String status;
        switch (order.getStatus() == null ? -1 : order.getStatus()) {
            case 0:  status = "进行中"; break;
            case 1:  status = "✅ 成功"; break;
            case 2:  status = "取消";   break;
            case 3:  status = "失败";   break;
            default: status = "未知(" + order.getStatus() + ")";
        }
        String sb = "📋 " + label + "订单查询\n" +
                "订单号: " + order.getOrderId() + "\n" +
                "状态: " + status + "\n" +
                "请求金额: " + fmt(order.getReqAmount()) + "\n" +
                "实际金额: " + fmt(order.getRealAmount()) + "\n" +
                "UTR: " + nvl(order.getOtherOrderId()) + "\n" +
                "创建时间: " + fmtDate(order.getCreateTime()) + "\n" +
                "支付时间: " + fmtDate(order.getPayTime());
        sendMessage((byte) 0, chatId, sb);
    }

    private void handleUtrQuery(long chatId, String utr) {
        Map<String, Object> record = extBankPayUtrMapper.searchBankPayByUtr(utr);
        if (record == null) {
            sendMessage((byte) 0, chatId, "❌ 未找到 UTR 记录: " + utr);
            return;
        }
        Object statusVal = record.get("status");
        String statusStr = "0".equals(String.valueOf(statusVal)) ? "未处理" : "✅ 成功";
        String sb = "🔍 UTR 记录\n" +
                "UTR: " + nvl(record.get("utr")) + "\n" +
                "状态: " + statusStr + "\n" +
                "商户ID: " + nvl(record.get("platformId")) + "\n" +
                "创建时间: " + nvl(record.get("createTime")) + "\n" +
                "备注: " + nvl(record.get("remark"));
        sendMessage((byte) 0, chatId, sb);
    }

    private void handleManualCallback(long chatId, String orderId) {
        try {
            manualCallbackService.manualCallback(orderId);
            sendMessage((byte) 0, chatId,
                    "orderId: " + orderId + "\ncode: 200\nmsg: success");
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendMessage((byte) 0, chatId,
                    "orderId: " + orderId + "\ncode: 500\nmsg: " + e.getMessage());
        } catch (Exception e) {
            log.error("手动回调异常 orderId={}", orderId, e);
            sendMessage((byte) 0, chatId,
                    "orderId: " + orderId + "\ncode: 500\nmsg: " + e.getMessage());
        }
    }

    private void handleUtrSupply(long chatId, String orderId, String utr) {
        int rows = orderInfoMapper.updateOtherOrderId(orderId, utr);
        if (rows == 0) {
            sendMessage((byte) 0, chatId, "⚠️ 未找到订单 " + orderId + "，继续尝试触发回调...");
        }
        try {
            manualCallbackService.manualCallback(orderId);
            sendMessage((byte) 0, chatId, "✅ UTR补单成功: " + orderId + " → " + utr);
        } catch (Exception e) {
            sendMessage((byte) 0, chatId, "❌ 补单失败: " + e.getMessage());
        }
    }

    private void handleTodayAccounts(long chatId) {
        List<Integer> pids = orderInfoMapper.selectTodayDistinctPlatformIds();
        if (pids == null || pids.isEmpty()) {
            sendMessage((byte) 0, chatId, "今日暂无收款");
            return;
        }

        // Account names
        Map<Integer, String> accountMap = Collections.emptyMap();
        try {
            R<List<PlatformAccountDTO>> acctResp = adminUserAuthClient.getAccountsByPlatformIds(pids);
            if (acctResp != null && acctResp.getData() != null) {
                accountMap = acctResp.getData().stream()
                        .filter(a -> a.getPlatformId() != null)
                        .collect(Collectors.toMap(PlatformAccountDTO::getPlatformId,
                                a -> a.getAccount() != null ? a.getAccount() : String.valueOf(a.getPlatformId()),
                                (x, y) -> x));
            }
        } catch (Exception e) {
            log.warn("获取商户账号失败", e);
        }

        // Balances
        Map<Integer, MerchantBalanceDTO> balanceMap = Collections.emptyMap();
        try {
            R<List<MerchantBalanceDTO>> balResp = adminBalanceClient.getBalancesByPlatformIds(pids);
            if (balResp != null && balResp.getData() != null) {
                balanceMap = balResp.getData().stream()
                        .filter(b -> b.getPlatformId() != null && "INR".equals(b.getCurrency()))
                        .collect(Collectors.toMap(MerchantBalanceDTO::getPlatformId,
                                b -> b, (x, y) -> x));
            }
        } catch (Exception e) {
            log.warn("获取商户余额失败", e);
        }

        Map<Integer, String> finalAccountMap = accountMap;
        Map<Integer, MerchantBalanceDTO> finalBalanceMap = balanceMap;

        StringBuilder sb = new StringBuilder("📊 今日收款户\n");
        for (Integer pid : pids) {
            String account = finalAccountMap.getOrDefault(pid, String.valueOf(pid));
            MerchantBalanceDTO bal = finalBalanceMap.get(pid);
            sb.append("\n商户:").append(account).append("\n");
            if (bal != null) {
                sb.append("INR 余额:").append(fmt(bal.getAvailable()))
                  .append(",待结算:").append(fmt(bal.getFrozen())).append("\n");
            } else {
                sb.append("INR 余额:--,待结算:--\n");
            }
        }

        // Telegram 单条消息上限 4096 字符，超长则分批发送
        String msg = sb.toString();
        if (msg.length() <= 4096) {
            sendMessage((byte) 0, chatId, msg);
        } else {
            int start = 0;
            while (start < msg.length()) {
                int end = Math.min(start + 4000, msg.length());
                sendMessage((byte) 0, chatId, msg.substring(start, end));
                start = end;
            }
        }
    }

    private void handleSuccessRate(long chatId, String[] parts) {
        int minutes = 5;
        if (parts.length >= 2) {
            try {
                minutes = Integer.parseInt(parts[1]);
                if (minutes <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sendMessage((byte) 0, chatId, "❌ 用法: /r [分钟数]，例如 /r 5 或 /r 30");
                return;
            }
        }

        List<MerchantRateDTO> rates = dashboardMetricsService.getMerchantSuccessRates(minutes);
        if (rates.isEmpty()) {
            sendMessage((byte) 0, chatId, "最近 " + minutes + " 分钟暂无代收订单");
            return;
        }

        // 批量获取账号名
        List<Integer> pids = rates.stream().map(MerchantRateDTO::getPlatformId).collect(Collectors.toList());
        Map<Integer, String> accountMap = Collections.emptyMap();
        try {
            R<List<PlatformAccountDTO>> acctResp = adminUserAuthClient.getAccountsByPlatformIds(pids);
            if (acctResp != null && acctResp.getData() != null) {
                accountMap = acctResp.getData().stream()
                        .filter(a -> a.getPlatformId() != null)
                        .collect(Collectors.toMap(PlatformAccountDTO::getPlatformId,
                                a -> a.getAccount() != null ? a.getAccount() : String.valueOf(a.getPlatformId()),
                                (x, y) -> x));
            }
        } catch (Exception e) {
            log.warn("获取商户账号失败", e);
        }

        Map<Integer, String> finalAccountMap = accountMap;
        int finalMinutes = minutes;
        StringBuilder sb = new StringBuilder();
        for (MerchantRateDTO r : rates) {
            if (sb.length() > 0) sb.append("\n=======\n");
            String account = finalAccountMap.getOrDefault(r.getPlatformId(), String.valueOf(r.getPlatformId()));
            sb.append("商户:").append(account).append("\n");
            sb.append("最近").append(finalMinutes).append("分钟成功率 ")
              .append(r.getSuccessRate() != null ? r.getSuccessRate().toPlainString() : "0.00").append("%");
        }

        String msg = sb.toString();
        if (msg.length() <= 4096) {
            sendMessage((byte) 0, chatId, msg);
        } else {
            int start = 0;
            while (start < msg.length()) {
                int end = Math.min(start + 4000, msg.length());
                sendMessage((byte) 0, chatId, msg.substring(start, end));
                start = end;
            }
        }
    }

    // ── 旧指令（保留） ────────────────────────────────────────────────────

    private void handleQueryCommand(long chatId) {
        List<MerchantBalanceDTO> balances;
        try {
            R<List<MerchantBalanceDTO>> resp = adminBalanceClient.getAllBalances();
            balances = (resp != null && resp.getData() != null) ? resp.getData() : Collections.emptyList();
        } catch (Exception e) {
            log.error("获取所有余额失败", e);
            sendMessage((byte) 0, chatId, "❌ 查询余额失败: " + e.getMessage());
            return;
        }
        if (balances.isEmpty()) {
            sendMessage((byte) 0, chatId, "暂无余额数据");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (MerchantBalanceDTO b : balances) {
            if (sb.length() > 0) sb.append("\n=======\n");
            sb.append("商户:").append(nvl(b.getAccount())).append("\n");
            sb.append("INR 余额:").append(fmt(b.getAvailable()))
              .append(",待结算:").append(fmt(b.getFrozen()));
        }
        String msg = sb.toString();
        if (msg.length() <= 4096) {
            sendMessage((byte) 0, chatId, msg);
        } else {
            int start = 0;
            while (start < msg.length()) {
                int end = Math.min(start + 4000, msg.length());
                sendMessage((byte) 0, chatId, msg.substring(start, end));
                start = end;
            }
        }
    }

    private void handleQueryCommandC(long chatId) {
        String apiUrl = "https://my.i-pay.cc/api.php?act=settle&pid=1172&key=120u10zeN6c7ORO66C7FoZ21c0Y1rhZ1";
        try {
            String responseData = restTemplate.getForObject(apiUrl, String.class);
            sendMessage((byte) 0, chatId, "🔍 查询结果：\n" + responseData);
        } catch (Exception e) {
            log.error(">>> 接口调用失败: ", e);
            sendMessage((byte) 0, chatId, "❌ 接口查询异常，请检查网络或配置");
        }
    }

    // ── Bot 基础 ─────────────────────────────────────────────────────────

    public void run() {
        if (telegramBotToken == null || telegramBotToken.isEmpty()) {
            log.error("启动失败：Token 未正确配置");
            return;
        }
        log.info("-------------------------- " + telegramBotToken);
        try {
            if (this.bot != null) {
                this.bot.removeGetUpdatesListener();
            }
            this.bot = new TelegramBot(telegramBotToken);
            // Brief pause so Telegram can drop any previous long-poll connection (avoids 409)
            Thread.sleep(3000);
            this.bot.setUpdatesListener(this);
            log.info(">>>>>> Telegram Bot 监听器注册成功！");
        } catch (Exception e) {
            log.error(">>>>>> Telegram Bot 启动异常: ", e);
        }
    }

    public void sendMessage(Byte type, long chatId, String text) {
        if (bot == null) {
            log.error("Bot 尚未初始化，无法发送消息");
            return;
        }
        SendResponse response;
        if (type == 1) {
            response = bot.execute(new SendPhoto(chatId, text));
        } else {
            response = bot.execute(new SendMessage(chatId, text));
        }
        if (!response.isOk()) {
            log.error("消息发送失败: {}", response.description());
        }
    }

    public void close() {
        if (this.bot != null) {
            this.bot.removeGetUpdatesListener();
        }
    }

    // ── 格式化工具 ────────────────────────────────────────────────────────

    private static String fmt(BigDecimal v) {
        return v == null ? "--" : v.toPlainString();
    }

    private static String nvl(Object v) {
        return v == null || v.toString().isBlank() ? "--" : v.toString();
    }

    private static String fmtDate(java.util.Date d) {
        if (d == null) return "--";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }
}
