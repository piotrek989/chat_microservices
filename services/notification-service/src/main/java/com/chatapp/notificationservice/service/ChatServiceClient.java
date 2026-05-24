package com.chatapp.notificationservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ChatServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(ChatServiceClient.class);
    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<List<String>>() {};

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String baseUrl;

    public ChatServiceClient(ObjectMapper objectMapper,
                             @Value("${chat-service.base-url}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    public List<String> getParticipantIds(String chatId) {
        if (chatId == null || chatId.isBlank() || baseUrl.isBlank()) {
            return Collections.emptyList();
        }
        try {
            String encodedChatId = URLEncoder.encode(chatId, StandardCharsets.UTF_8);
            URI uri = URI.create(baseUrl + "/internal/chats/" + encodedChatId + "/participants");
            HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                logger.warn("Chat service returned {} when fetching participants for chat {}",
                    response.statusCode(), chatId);
                return Collections.emptyList();
            }
            String body = response.body();
            if (body == null || body.isBlank()) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(body, LIST_OF_STRINGS);
        } catch (Exception ex) {
            logger.error("Failed to fetch chat participants for chat {}", chatId, ex);
            return Collections.emptyList();
        }
    }
}
