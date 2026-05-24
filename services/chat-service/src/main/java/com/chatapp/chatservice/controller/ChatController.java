package com.chatapp.chatservice.controller;

import com.chatapp.chatservice.dto.ChatSummaryDto;
import com.chatapp.chatservice.dto.CreateChatRequest;
import com.chatapp.chatservice.dto.CreateChatResponse;
import com.chatapp.chatservice.dto.MessageDto;
import com.chatapp.chatservice.dto.SendMessageRequest;
import com.chatapp.chatservice.dto.SendMessageResponse;
import com.chatapp.chatservice.service.ChatService;
import com.chatapp.chatservice.service.CurrentUserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chats")
public class ChatController {
    private final ChatService chatService;
    private final CurrentUserService currentUserService;

    public ChatController(ChatService chatService, CurrentUserService currentUserService) {
        this.chatService = chatService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<CreateChatResponse> createChat(@Valid @RequestBody CreateChatRequest request) {
        String userId = currentUserService.getCurrentUserId();
        UUID chatId = chatService.createChat(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateChatResponse(chatId));
    }

    @GetMapping
    public ResponseEntity<List<ChatSummaryDto>> getUserChats() {
        String userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(chatService.getUserChats(userId));
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<SendMessageResponse> sendMessage(
        @PathVariable UUID chatId,
        @Valid @RequestBody SendMessageRequest request
    ) {
        String userId = currentUserService.getCurrentUserId();
        String username = currentUserService.getCurrentUsername();
        MessageDto message = chatService.sendMessage(chatId, userId, username, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new SendMessageResponse(message.getMessageId(), message.getCreatedAt()));
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable UUID chatId) {
        String userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(chatService.getMessages(chatId, userId));
    }
}
