package com.example.smarthome.server.telegram;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public class ReplyKeyboardMessage extends Message {
    private List<String> buttons;
    private int numOfColumns = 1;
    private boolean oneTimeKeyboard;

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

    public ReplyKeyboardMessage hasOneTimeKeyboard(boolean b) {
        this.oneTimeKeyboard = b;
        return this;
    }

    public ReplyKeyboardMessage setMessageId(int messageId) {
        super.setMessageId(messageId);
        return this;
    }
}
