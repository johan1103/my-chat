package org.example;

public enum MessagePrefix {
    STRING("string:"), SERVER_MESSAGE("server_message:");

    private final String type;
    MessagePrefix(String type) {
        this.type = type;
    }
    String getType(){
        return type;
    }
}
