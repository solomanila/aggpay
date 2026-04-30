package com.letsvpn.pay.task;

import com.letsvpn.pay.task.TelegramBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TelegramStartedBootstrap implements ApplicationListener<ContextRefreshedEvent> {

    private final TelegramBotService telegramBotService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (contextRefreshedEvent.getApplicationContext().getParent() == null) {
            telegramBotService.run();
        }
    }
}