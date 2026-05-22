package com.chatapp.chatservice.dto;

import java.util.UUID;

public class CreateChatResponse {
    private UUID chatId;

    public CreateChatResponse(UUID chatId) {
        this.chatId = chatId;
    }

    public UUID getChatId() {
        return chatId;
    }
}
