package com.chatapp.chatservice.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_participants")
public class ChatParticipant {
    @EmbeddedId
    private ChatParticipantId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chatId")
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    public ChatParticipant() {
    }

    public ChatParticipant(Chat chat, String userId) {
        this.chat = chat;
        this.id = new ChatParticipantId(chat.getId(), userId);
    }

    public ChatParticipantId getId() {
        return id;
    }

    public String getUserId() {
        return id != null ? id.getUserId() : null;
    }
}
