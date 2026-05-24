package com.chatapp.notificationservice.dto;

import java.time.Instant;
import java.util.UUID;

public class NotificationResponse {
    private UUID id;
    private Instant createdAt;

    public NotificationResponse(UUID id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
