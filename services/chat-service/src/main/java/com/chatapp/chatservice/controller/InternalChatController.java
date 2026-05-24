package com.chatapp.chatservice.controller;

import com.chatapp.chatservice.service.ChatService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/chats")
public class InternalChatController {
    private final ChatService chatService;

    public InternalChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/{chatId}/participants")
    public ResponseEntity<List<String>> getParticipants(@PathVariable UUID chatId) {
        return ResponseEntity.ok(chatService.getParticipantIds(chatId));
    }
}
