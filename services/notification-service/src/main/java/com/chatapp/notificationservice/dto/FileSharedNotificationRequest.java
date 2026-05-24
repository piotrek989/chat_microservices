package com.chatapp.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;

public class FileSharedNotificationRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String chatId;
    @NotBlank
    private String fileId;

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

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
}
