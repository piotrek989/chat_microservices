package com.chatapp.chatservice.service;

import com.chatapp.chatservice.dto.CreateChatRequest;
import com.chatapp.chatservice.dto.SendMessageRequest;
import com.chatapp.chatservice.entity.Chat;
import com.chatapp.chatservice.entity.ChatParticipant;
import com.chatapp.chatservice.entity.Message;
import com.chatapp.chatservice.repository.ChatParticipantRepository;
import com.chatapp.chatservice.repository.ChatRepository;
import com.chatapp.chatservice.repository.MessageRepository;
import com.chatapp.chatservice.sns.NotificationType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {
    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ChatParticipantRepository chatParticipantRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private SnsPublisherService snsPublisherService;

    @InjectMocks
    private ChatService chatService;

    @Test
    void createChatAddsCurrentUserAndDoesNotDuplicateParticipants() {
        CreateChatRequest request = new CreateChatRequest();
        request.setParticipantIds(List.of("alice", "bob"));
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID chatId = chatService.createChat("alice", request);

        ArgumentCaptor<Chat> chatCaptor = ArgumentCaptor.forClass(Chat.class);
        verify(chatRepository).save(chatCaptor.capture());
        Chat savedChat = chatCaptor.getValue();
        Set<String> participantIds = savedChat.getParticipants().stream()
            .map(ChatParticipant::getUserId)
            .collect(Collectors.toSet());
        assertEquals(savedChat.getId(), chatId);
        assertEquals(Set.of("alice", "bob"), participantIds);
    }

    @Test
    void sendMessageSavesMessageAndPublishesNotificationForParticipant() {
        UUID chatId = UUID.randomUUID();
        Chat chat = new Chat(chatId);
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Hello");
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(chatParticipantRepository.existsByIdChatIdAndIdUserId(chatId, "alice")).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = chatService.sendMessage(chatId, "alice", "Alice", request);

        assertEquals(chatId, result.getChatId());
        assertEquals("alice", result.getSenderId());
        assertEquals("Alice", result.getSenderUsername());
        assertEquals("Hello", result.getContent());
        verify(snsPublisherService).publishEvent(any(NotificationType.class), any());
    }

    @Test
    void sendMessageRejectsNonParticipantWithoutSavingOrPublishing() {
        UUID chatId = UUID.randomUUID();
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(new Chat(chatId)));
        when(chatParticipantRepository.existsByIdChatIdAndIdUserId(chatId, "mallory")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> chatService.sendMessage(chatId, "mallory", "Mallory", new SendMessageRequest())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(messageRepository, never()).save(any(Message.class));
        verify(snsPublisherService, never()).publishEvent(any(), any());
    }
}
