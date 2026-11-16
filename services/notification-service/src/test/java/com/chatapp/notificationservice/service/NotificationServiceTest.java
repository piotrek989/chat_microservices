package com.chatapp.notificationservice.service;

import com.chatapp.notificationservice.entity.Notification;
import com.chatapp.notificationservice.entity.NotificationType;
import com.chatapp.notificationservice.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ChatServiceClient chatServiceClient;

    private NotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, objectMapper, chatServiceClient);
    }

    @Test
    void processSnsEventNotifiesOtherChatParticipants() throws Exception {
        when(chatServiceClient.getParticipantIds("chat-1")).thenReturn(List.of("sender", "recipient", " "));

        String messagePayload = objectMapper.writeValueAsString(java.util.Map.of(
            "type", "MESSAGE",
            "data", java.util.Map.of(
                "userId", "sender",
                "chatId", "chat-1",
                "messageId", "message-1"
            )
        ));
        String snsEvent = objectMapper.writeValueAsString(java.util.Map.of("Message", messagePayload));

        notificationService.processSnsEvent(snsEvent);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertEquals(NotificationType.MESSAGE, notification.getType());
        assertEquals("recipient", notification.getUserId());
        assertEquals("chat-1", notification.getChatId());
        assertEquals("message-1", notification.getMessageId());
        assertEquals("New message", notification.getTitle());
    }

    @Test
    void processSnsEventSkipsWhenMessagePayloadIsMissing() throws Exception {
        String snsEvent = objectMapper.writeValueAsString(java.util.Map.of("Message", " "));

        notificationService.processSnsEvent(snsEvent);

        verify(chatServiceClient, never()).getParticipantIds(org.mockito.ArgumentMatchers.anyString());
        verify(notificationRepository, never()).save(org.mockito.ArgumentMatchers.any(Notification.class));
    }
}
