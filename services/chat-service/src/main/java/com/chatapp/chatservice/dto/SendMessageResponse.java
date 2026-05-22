package com.chatapp.chatservice.dto;

import java.time.Instant;
import java.util.UUID;

public class SendMessageResponse {
    private UUID messageId;
    private Instant createdAt;

    public SendMessageResponse(UUID messageId, Instant createdAt) {
        this.messageId = messageId;
        this.createdAt = createdAt;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
