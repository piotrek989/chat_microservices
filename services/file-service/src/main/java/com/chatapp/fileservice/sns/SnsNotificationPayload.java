package com.chatapp.fileservice.sns;

public class SnsNotificationPayload {
    private NotificationType type;
    private Object data;

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
