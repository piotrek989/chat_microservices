package com.chatapp.chatservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ChatParticipantId implements Serializable {
    @Column(name = "chat_id", columnDefinition = "uuid")
    private UUID chatId;

    @Column(name = "user_id")
    private String userId;

    public ChatParticipantId() {
    }

    public ChatParticipantId(UUID chatId, String userId) {
        this.chatId = chatId;
        this.userId = userId;
    }

    public UUID getChatId() {
        return chatId;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChatParticipantId that = (ChatParticipantId) o;
        return Objects.equals(chatId, that.chatId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatId, userId);
    }
}
