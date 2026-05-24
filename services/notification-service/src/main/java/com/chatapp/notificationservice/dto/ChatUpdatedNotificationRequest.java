package com.chatapp.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatUpdatedNotificationRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String chatId;
    @NotBlank
    private String updateType;

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

    public String getUpdateType() {
        return updateType;
    }

    public void setUpdateType(String updateType) {
        this.updateType = updateType;
    }
}
