package com.chatapp.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SnsMessage {
    @JsonProperty("Type")
    private String type;
    @JsonProperty("Message")
    private String message;
    @JsonProperty("SubscribeURL")
    private String subscribeUrl;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSubscribeUrl() {
        return subscribeUrl;
    }

    public void setSubscribeUrl(String subscribeUrl) {
        this.subscribeUrl = subscribeUrl;
    }
}
