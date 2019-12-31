package com.example.smarthome.server.telegram;

import java.util.Arrays;
import java.util.List;

public class ReplyKeyboardMessage extends Message {
    private List<String> buttons;
    private int numOfColumns = 1;

    public ReplyKeyboardMessage(long chatId, String text, List<String> buttons) {
        super(chatId, text);
        this.buttons = buttons;
    }

    public ReplyKeyboardMessage(long chatId, String text, String[] buttons) {
        super(chatId, text);
        this.buttons = Arrays.asList(buttons);
    }

    public ReplyKeyboardMessage setNumOfColumns(int numOfColumns) {
        this.numOfColumns = numOfColumns;
        return this;
    }

    public int getNumOfColumns() {
        return numOfColumns;
    }

    public List<String> getButtons() {
        return buttons;
    }

    public ReplyKeyboardMessage hasAddButton(boolean b) {
        super.hasAddButton(b);
        return this;
    }

    public ReplyKeyboardMessage hasBackButton(boolean b) {
        super.hasBackButton(b);
        return this;
    }

    public ReplyKeyboardMessage hasRemoveButton(boolean b) {
        super.hasRemoveButton(b);
        return this;
    }

    public ReplyKeyboardMessage setMessageId(int messageId) {
        super.setMessageId(messageId);
        return this;
    }
}
