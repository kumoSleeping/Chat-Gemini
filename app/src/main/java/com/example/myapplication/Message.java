package com.example.myapplication;

public class Message {
    private final String content;
    private final boolean isUserMessage;

    public Message(String content, boolean isUserMessage) {
        this.content = content;
        this.isUserMessage = isUserMessage;
    }

    public String getContent() {
        return content;
    }

    public boolean isUserMessage() {
        return isUserMessage;
    }
}
