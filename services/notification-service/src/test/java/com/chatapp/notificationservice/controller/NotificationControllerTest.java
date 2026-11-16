package com.chatapp.notificationservice.controller;

import com.chatapp.notificationservice.service.CurrentUserService;
import com.chatapp.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class NotificationControllerTest {
    @Test
    void failedSnsSubscriptionConfirmationReturnsServiceUnavailableForRetry() {
        NotificationController controller = new NotificationController(
            mock(NotificationService.class),
            mock(CurrentUserService.class),
            new ObjectMapper()
        );

        var response = controller.handleSnsEvent(
            "SubscriptionConfirmation",
            "{\"SubscribeURL\":\"not-a-valid-uri\"}"
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }
}
