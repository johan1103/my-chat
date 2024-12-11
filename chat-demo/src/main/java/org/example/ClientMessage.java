package org.example;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

public class ClientMessage {
    @JsonProperty("userId")
    private String userId;
    @JsonProperty("text")
    private String text;

    public ClientMessage() {
    }

    public ClientMessage(String userId, String text) {
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
