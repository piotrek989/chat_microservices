package com.chatapp.chatservice.dto;

import java.time.Instant;
import java.util.UUID;

public class ChatSummaryDto {
    private UUID chatId;
    private Instant createdAt;
    private LastMessageDto lastMessage;

    public ChatSummaryDto(UUID chatId, Instant createdAt, LastMessageDto lastMessage) {
        this.chatId = chatId;
        this.createdAt = createdAt;
        this.lastMessage = lastMessage;
    }

    public UUID getChatId() {
        return chatId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public LastMessageDto getLastMessage() {
        return lastMessage;
    }
}
