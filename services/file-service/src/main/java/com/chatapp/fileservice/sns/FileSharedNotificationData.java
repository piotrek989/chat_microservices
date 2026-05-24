package com.chatapp.fileservice.sns;

public class FileSharedNotificationData {
    private String userId;
    private String chatId;
    private String fileId;

    public FileSharedNotificationData() {
    }

    public FileSharedNotificationData(String userId, String chatId, String fileId) {
        this.userId = userId;
        this.chatId = chatId;
        this.fileId = fileId;
    }

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
