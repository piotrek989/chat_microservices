package com.chatapp.notificationservice.service;

import com.chatapp.notificationservice.dto.FileSharedNotificationRequest;
import com.chatapp.notificationservice.dto.NewChatMessageNotificationRequest;
import com.chatapp.notificationservice.dto.SnsMessage;
import com.chatapp.notificationservice.dto.SnsNotificationPayload;
import com.chatapp.notificationservice.entity.Notification;
import com.chatapp.notificationservice.entity.NotificationType;
import com.chatapp.notificationservice.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final ChatServiceClient chatServiceClient;

    public NotificationService(
        NotificationRepository notificationRepository,
        ObjectMapper objectMapper,
        ChatServiceClient chatServiceClient
    ) {
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
        this.chatServiceClient = chatServiceClient;
    }

    public Notification createForNewMessage(NewChatMessageNotificationRequest request) {
        String title = "New message";
        String body = "New message in chat " + request.getChatId() + ".";
        Notification notification = baseNotification(NotificationType.MESSAGE, request.getUserId(), title, body);
        notification.setChatId(request.getChatId());
        notification.setMessageId(request.getMessageId());
        return saveAndPublish(notification);
    }

    public Notification createForFileShared(FileSharedNotificationRequest request) {
        String title = "Photo shared";
        String body = "New photo in chat " + request.getChatId() + ".";
        Notification notification = baseNotification(NotificationType.FILE_SHARED, request.getUserId(), title, body);
        notification.setChatId(request.getChatId());
        notification.setFileId(request.getFileId());
        return saveAndPublish(notification);
    }

    public List<Notification> getHistoryByType(NotificationType type) {
        return notificationRepository.findByTypeOrderByCreatedAtDesc(type);
    }

    public List<Notification> getHistoryByTypeAndUser(NotificationType type, String userId) {
        return notificationRepository.findByTypeAndUserIdOrderByCreatedAtDesc(type, userId);
    }

    public void processSnsEvent(String messageBody) {
        try {
            SnsMessage snsMessage = objectMapper.readValue(messageBody, SnsMessage.class);
            String innerMessage = snsMessage.getMessage();
            if (innerMessage == null || innerMessage.isBlank()) {
                logger.warn("SNS event missing Message payload");
                return;
            }
            SnsNotificationPayload payload = objectMapper.readValue(innerMessage, SnsNotificationPayload.class);
            NotificationType type = payload.getType();
            if (type == null) {
                logger.warn("SNS event missing notification type");
                return;
            }
            switch (type) {
                case MESSAGE -> {
                    NewChatMessageNotificationRequest request = objectMapper.convertValue(
                        payload.getData(),
                        NewChatMessageNotificationRequest.class
                    );
                    createForMessageRecipients(request);
                }
                case FILE_SHARED -> {
                    FileSharedNotificationRequest request = objectMapper.convertValue(
                        payload.getData(),
                        FileSharedNotificationRequest.class
                    );
                    createForFileRecipients(request);
                }
                default -> logger.warn("Unsupported notification type: {}", type);
            }
        } catch (Exception ex) {
            logger.error("Failed to process SNS event", ex);
            throw new IllegalStateException("Failed to process SNS event", ex);
        }
    }

    private Notification baseNotification(NotificationType type, String userId, String title, String body) {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setType(type);
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setCreatedAt(Instant.now());
        return notification;
    }

    private Notification saveAndPublish(Notification notification) {
        return notificationRepository.save(notification);
    }

    private void createForMessageRecipients(NewChatMessageNotificationRequest request) {
        if (request == null || request.getChatId() == null) {
            logger.warn("Message notification missing chatId");
            return;
        }
        List<String> participantIds = chatServiceClient.getParticipantIds(request.getChatId());
        if (participantIds.isEmpty()) {
            logger.warn("No chat participants resolved for message notification in chat {}", request.getChatId());
            return;
        }
        for (String participantId : participantIds) {
            if (participantId == null || participantId.isBlank()) {
                continue;
            }
            if (participantId.equals(request.getUserId())) {
                continue;
            }
            Notification notification = baseNotification(
                NotificationType.MESSAGE,
                participantId,
                "New message",
                "New message in chat " + request.getChatId() + "."
            );
            notification.setChatId(request.getChatId());
            notification.setMessageId(request.getMessageId());
            saveAndPublish(notification);
        }
    }

    private void createForFileRecipients(FileSharedNotificationRequest request) {
        if (request == null || request.getChatId() == null) {
            logger.warn("File notification missing chatId");
            return;
        }
        List<String> participantIds = chatServiceClient.getParticipantIds(request.getChatId());
        if (participantIds.isEmpty()) {
            logger.warn("No chat participants resolved for file notification in chat {}", request.getChatId());
            return;
        }
        for (String participantId : participantIds) {
            if (participantId == null || participantId.isBlank()) {
                continue;
            }
            if (participantId.equals(request.getUserId())) {
                continue;
            }
            Notification notification = baseNotification(
                NotificationType.FILE_SHARED,
                participantId,
                "Photo shared",
                "New photo in chat " + request.getChatId() + "."
            );
            notification.setChatId(request.getChatId());
            notification.setFileId(request.getFileId());
            saveAndPublish(notification);
        }
    }
}
