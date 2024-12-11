package org.example;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ServerMessage{
    private String userId;
    private String text;
    private String messageSendTime;

    public ServerMessage(String userId, String text, String messageSendTime) {
        this.userId = userId;
        this.text = text;
        this.messageSendTime = messageSendTime;
    }

    public ServerMessage() {
    }

    public static ServerMessage of(String userId, ClientMessage clientMessage) {
        return new ServerMessage(userId, clientMessage.getText(), ZonedDateTime.now(ZoneId.of(ZoneId.SHORT_IDS.get("JST")))
                                                                                                  .format(DateTimeFormatter.ISO_DATE_TIME));
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMessageSendTime() {
        return messageSendTime;
    }

    public void setMessageSendTime(String messageSendTime) {
        this.messageSendTime = messageSendTime;
    }
}
