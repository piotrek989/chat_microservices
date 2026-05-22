package com.chatapp.chatservice.dto;

import java.time.Instant;
import java.util.UUID;

public class MessageDto {
    private UUID messageId;
    private UUID chatId;
    private String senderId;
    private String content;
    private Instant createdAt;

    public MessageDto(UUID messageId, UUID chatId, String senderId, String content, Instant createdAt) {
        this.messageId = messageId;
        this.chatId = chatId;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public UUID getChatId() {
        return chatId;
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
