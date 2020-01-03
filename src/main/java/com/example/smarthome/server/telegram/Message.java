package com.example.smarthome.server.telegram;

import lombok.Getter;

@Getter
public class Message {
    private String text;
    private long chatId;
    private int messageId;
    private boolean addButton = false;
    private boolean removeButton = false;
    private boolean backButton = false;

    public Message(long chatId, String text) {
        this.text = text;
        this.chatId = chatId;
    }

    public Message hasAddButton(boolean b) {
        addButton = b;
        return this;
    }

    public Message hasBackButton(boolean b) {
        backButton = b;
        return this;
    }

    public Message hasRemoveButton(boolean b) {
        removeButton = b;
        return this;
    }

    public Message setMessageId(int messageId) {
        this.messageId = messageId;
        return this;
    }
}
