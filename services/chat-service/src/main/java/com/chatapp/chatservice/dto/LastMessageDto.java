package com.chatapp.chatservice.dto;

import java.time.Instant;
import java.util.UUID;

public class LastMessageDto {
    private UUID messageId;
    private String senderId;
    private String content;
    private Instant createdAt;

    public LastMessageDto(UUID messageId, String senderId, String content, Instant createdAt) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
