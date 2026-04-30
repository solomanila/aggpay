package com.letsvpn.pay.task;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TelegramBotService implements UpdatesListener {

    @Value("${telegram-bot.token}")
    private String telegramBotToken;

    private TelegramBot bot;

    // 注入 RestTemplate 用于调用外部接口
    // 如果启动报错找不到这个 Bean，请在主启动类或配置类中定义: @Bean public RestTemplate restTemplate() { return new RestTemplate(); }
    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        log.info(">>>>>> 正在准备启动 Telegram Bot...");
        this.run();
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            if (update.message() != null && update.message().text() != null) {
                String text = update.message().text().trim();
                long chatId = update.message().chat().id();
                log.info(">>> 收到消息: {} 来自 ChatID: {}", text, chatId);

                // 判断指令是否为 /b
                if ("/b".equalsIgnoreCase(text)) {
                    handleQueryCommand(chatId);
                }

                // 判断指令是否为 /b
                if ("/c".equalsIgnoreCase(text)) {
                    handleQueryCommandC(chatId);
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    /**
     * 处理 /b 指令：调用接口并返回结果
     */
    private void handleQueryCommand(long chatId) {
        String apiUrl = "https://my.i-pay.cc/api.php?act=query&pid=1172&key=120u10zeN6c7ORO66C7FoZ21c0Y1rhZ1";

        try {
            log.info(">>> 正在调用接口: {}", apiUrl);
            // 调用接口并获取返回的字符串数据
            String responseData = restTemplate.getForObject(apiUrl, String.class);

            // 将接口数据发送回群组
            String messageText = "🔍 查询结果：\n" + responseData;
            this.sendMessage((byte) 0, chatId, messageText);

        } catch (Exception e) {
            log.error(">>> 接口调用失败: ", e);
            this.sendMessage((byte) 0, chatId, "❌ 接口查询异常，请检查网络或配置");
        }
    }


    /**
     * 处理 /c 指令：调用接口并返回结果
     */
    private void handleQueryCommandC(long chatId) {
        String apiUrl = "https://my.i-pay.cc/api.php?act=settle&pid=1172&key=120u10zeN6c7ORO66C7FoZ21c0Y1rhZ1";

        try {
            log.info(">>> 正在调用接口: {}", apiUrl);
            // 调用接口并获取返回的字符串数据
            String responseData = restTemplate.getForObject(apiUrl, String.class);

            // 将接口数据发送回群组
            String messageText = "🔍 查询结果：\n" + responseData;
            this.sendMessage((byte) 0, chatId, messageText);

        } catch (Exception e) {
            log.error(">>> 接口调用失败: ", e);
            this.sendMessage((byte) 0, chatId, "❌ 接口查询异常，请检查网络或配置");
        }
    }

    public void run() {
        if (this.telegramBotToken == null || this.telegramBotToken.isEmpty()) {
            log.error("启动失败：Token 未正确配置");
            return;
        }

        log.info("-------------------------- " + telegramBotToken);

        try {
            this.bot = new TelegramBot(this.telegramBotToken);
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
}