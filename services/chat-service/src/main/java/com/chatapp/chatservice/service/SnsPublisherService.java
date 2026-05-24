package com.chatapp.chatservice.service;

import com.chatapp.chatservice.sns.NotificationType;
import com.chatapp.chatservice.sns.SnsNotificationPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Service
public class SnsPublisherService {
    private static final Logger logger = LoggerFactory.getLogger(SnsPublisherService.class);

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final String topicArn;

    public SnsPublisherService(
        SnsClient snsClient,
        ObjectMapper objectMapper,
        @Value("${aws.sns-topic-arn:}") String topicArn
    ) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.topicArn = topicArn == null ? "" : topicArn;
    }

    public void publishEvent(NotificationType type, Object data) {
        if (topicArn.isBlank()) {
            logger.warn("SNS topic ARN is not configured; skipping publish");
            return;
        }
        try {
            SnsNotificationPayload payload = new SnsNotificationPayload();
            payload.setType(type);
            payload.setData(data);
            String message = objectMapper.writeValueAsString(payload);
            snsClient.publish(PublishRequest.builder()
                .topicArn(topicArn)
                .message(message)
                .build());
        } catch (Exception ex) {
            logger.error("Failed to publish SNS event type {}", type, ex);
        }
    }
}
