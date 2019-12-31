package com.example.smarthome.server.telegram;

public class Message {
    private String text;
    private Long chatId;
    private Integer messageId = 0;
    private boolean addButton = false;
    private boolean removeButton = false;
    private boolean backButton = false;

    public Message(Long chatId, String text) {
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

    public Message setMessageId(Integer messageId) {
        this.messageId = messageId;
        return this;
    }

    public String getText() {
        return text;
    }

    public Long getChatId() {
        return chatId;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public boolean isAddButton() {
        return addButton;
    }

    public boolean isRemoveButton() {
        return removeButton;
    }

    public boolean isBackButton() {
        return backButton;
    }
}
