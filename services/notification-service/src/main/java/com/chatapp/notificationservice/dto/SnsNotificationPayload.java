package com.chatapp.notificationservice.dto;

import com.chatapp.notificationservice.entity.NotificationType;
import com.fasterxml.jackson.databind.JsonNode;

public class SnsNotificationPayload {
    private NotificationType type;
    private JsonNode data;

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }
}
