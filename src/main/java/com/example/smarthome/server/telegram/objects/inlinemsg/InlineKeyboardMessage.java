package com.example.smarthome.server.telegram.objects.inlinemsg;

import com.example.smarthome.server.telegram.objects.Message;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class InlineKeyboardMessage extends Message {
    private List<CallbackButton> buttons;
    private int numOfColumns = 1;
    private boolean addButton = false;
    private boolean removeButton = false;
    private boolean backButton = false;

    public InlineKeyboardMessage(long chatId, String text, List<CallbackButton> buttons) {
        super(chatId, text);
        if (buttons == null) {
            this.buttons = new ArrayList<>();
        } else {
            this.buttons = buttons;
        }
    }

    public InlineKeyboardMessage setNumOfColumns(int numOfColumns) {
        this.numOfColumns = numOfColumns;
        return this;
    }

    public InlineKeyboardMessage hasAddButton(boolean b) {
        addButton = b;
        return this;
    }

    public InlineKeyboardMessage hasBackButton(boolean b) {
        backButton = b;
        return this;
    }

    public InlineKeyboardMessage hasRemoveButton(boolean b) {
        removeButton = b;
        return this;
    }

    public InlineKeyboardMessage setMessageId(int messageId) {
        super.setMessageId(messageId);
        return this;
    }
}
