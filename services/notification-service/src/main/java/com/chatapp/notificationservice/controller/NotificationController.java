package com.chatapp.notificationservice.controller;

import com.chatapp.notificationservice.dto.NotificationHistoryItem;
import com.chatapp.notificationservice.dto.SnsMessage;
import com.chatapp.notificationservice.entity.NotificationType;
import com.chatapp.notificationservice.service.CurrentUserService;
import com.chatapp.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public NotificationController(
        NotificationService notificationService,
        CurrentUserService currentUserService,
        ObjectMapper objectMapper
    ) {
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    @PostMapping("/sns")
    public ResponseEntity<Void> handleSnsEvent(
        @RequestHeader("x-amz-sns-message-type") String messageType,
        @RequestBody String messageBody
    ) {
        try {
            if ("SubscriptionConfirmation".equalsIgnoreCase(messageType)) {
                SnsMessage snsMessage = objectMapper.readValue(messageBody, SnsMessage.class);
                if (!confirmSubscription(snsMessage.getSubscribeUrl())) {
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
                }
                return ResponseEntity.ok().build();
            }
            if ("Notification".equalsIgnoreCase(messageType)) {
                notificationService.processSnsEvent(messageBody);
                return ResponseEntity.ok().build();
            }
        } catch (IOException ex) {
            logger.error("Failed to parse SNS message", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        logger.warn("Unsupported SNS message type: {}", messageType);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @GetMapping("/messages")
    public ResponseEntity<List<NotificationHistoryItem>> listMessageHistory() {
        String userId = currentUserService.getCurrentUserId();
        List<NotificationHistoryItem> history = notificationService.getHistoryByTypeAndUser(NotificationType.MESSAGE, userId)
            .stream()
            .map(NotificationHistoryItem::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/files")
    public ResponseEntity<List<NotificationHistoryItem>> listFileHistory() {
        String userId = currentUserService.getCurrentUserId();
        List<NotificationHistoryItem> history = notificationService.getHistoryByTypeAndUser(NotificationType.FILE_SHARED, userId)
            .stream()
            .map(NotificationHistoryItem::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    private boolean confirmSubscription(String subscribeUrl) {
        if (subscribeUrl == null || subscribeUrl.isBlank()) {
            logger.warn("SNS subscription confirmation missing SubscribeURL");
            return false;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(subscribeUrl))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 == 2) {
                logger.info("SNS subscription confirmed");
                return true;
            }
            logger.warn("SNS subscription confirmation failed with status {}", response.statusCode());
        } catch (Exception ex) {
            logger.error("Failed to confirm SNS subscription", ex);
        }
        return false;
    }
}
