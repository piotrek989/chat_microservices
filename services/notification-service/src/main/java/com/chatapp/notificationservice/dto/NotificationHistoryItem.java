package com.chatapp.notificationservice.dto;

import com.chatapp.notificationservice.entity.Notification;
import com.chatapp.notificationservice.entity.NotificationType;
import java.time.Instant;
import java.util.UUID;

public class NotificationHistoryItem {
    private UUID id;
    private NotificationType type;
    private String userId;
    private String chatId;
    private String messageId;
    private String fileId;
    private String title;
    private String body;
    private Instant createdAt;

    public static NotificationHistoryItem fromEntity(Notification notification) {
        NotificationHistoryItem item = new NotificationHistoryItem();
        item.id = notification.getId();
        item.type = notification.getType();
        item.userId = notification.getUserId();
        item.chatId = notification.getChatId();
        item.messageId = notification.getMessageId();
        item.fileId = notification.getFileId();
        item.title = notification.getTitle();
        item.body = notification.getBody();
        item.createdAt = notification.getCreatedAt();
        return item;
    }

    public UUID getId() {
        return id;
    }

    public NotificationType getType() {
        return type;
    }

    public String getUserId() {
        return userId;
    }

    public String getChatId() {
        return chatId;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getFileId() {
        return fileId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
