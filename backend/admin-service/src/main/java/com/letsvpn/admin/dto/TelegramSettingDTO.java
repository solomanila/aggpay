package com.letsvpn.admin.dto;

import lombok.Data;

@Data
public class TelegramSettingDTO {
    private String iconUrl;
    private String domain;
    private String telegramcustomservice;
    private String customerServiceEmail;
    private String customerServiceWhatsapp;
    private String telegramBotToken;
    private String telegramGroupId;
    private String payoutNotifyGroupId;
    private String telegramBotTokenSupplier;
    private Boolean telegramLoginVerification;
    private String telegramBotUsername;
    private String accountingGroupId;
}
