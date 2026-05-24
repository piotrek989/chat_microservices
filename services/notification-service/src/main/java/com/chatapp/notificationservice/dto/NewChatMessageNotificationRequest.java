package com.chatapp.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;

public class NewChatMessageNotificationRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String chatId;
    @NotBlank
    private String messageId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
