package com.chatapp.chatservice.sns;

public class MessageNotificationData {
    private String userId;
    private String chatId;
    private String messageId;

    public MessageNotificationData() {
    }

    public MessageNotificationData(String userId, String chatId, String messageId) {
        this.userId = userId;
        this.chatId = chatId;
        this.messageId = messageId;
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

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
