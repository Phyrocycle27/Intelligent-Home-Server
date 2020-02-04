package com.example.smarthome.server.telegram.objects;

import lombok.Getter;

@Getter
public class Message {
    private String text;
    private long chatId;
    private int messageId;

    public Message(long chatId, String text) {
        this.text = text;
        this.chatId = chatId;
    }

    public Message setMessageId(int messageId) {
        this.messageId = messageId;
        return this;
    }
}
