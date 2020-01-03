package com.example.smarthome.server.telegram;

import java.util.List;

public class InlineKeyboardMessage extends Message {
    private List<CallbackButton> buttons;
    private int numOfColumns = 1;

    public InlineKeyboardMessage(long chatId, String text, List<CallbackButton> buttons) {
        super(chatId, text);
        this.buttons = buttons;
    }

    public int getNumOfColumns() {
        return numOfColumns;
    }

    public List<CallbackButton> getButtons() {
        return buttons;
    }

    public InlineKeyboardMessage setNumOfColumns(int numOfColumns) {
        this.numOfColumns = numOfColumns;
        return this;
    }

    public InlineKeyboardMessage hasAddButton(boolean b) {
        super.hasAddButton(b);
        return this;
    }

    public InlineKeyboardMessage hasBackButton(boolean b) {
        super.hasBackButton(b);
        return this;
    }

    public InlineKeyboardMessage hasRemoveButton(boolean b) {
        super.hasRemoveButton(b);
        return this;
    }

    public InlineKeyboardMessage setMessageId(int messageId) {
        super.setMessageId(messageId);
        return this;
    }
}
