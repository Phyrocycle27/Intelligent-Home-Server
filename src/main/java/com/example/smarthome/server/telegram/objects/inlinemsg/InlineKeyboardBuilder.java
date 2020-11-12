package com.example.smarthome.server.telegram.objects.inlinemsg;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class InlineKeyboardBuilder {

    private long chatId;
    private int messageId;
    private String text;

    private final List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    private List<InlineKeyboardButton> row = new ArrayList<>();

    private InlineKeyboardBuilder() {
    }

    public static InlineKeyboardBuilder create(long chatId) {
        InlineKeyboardBuilder builder = new InlineKeyboardBuilder();
        builder.chatId = chatId;
        return builder;
    }

    public InlineKeyboardBuilder setText(String text) {
        this.text = text;
        return this;
    }

    public InlineKeyboardBuilder setMessageId(int messageId) {
        this.messageId = messageId;
        return this;
    }

    public InlineKeyboardBuilder row() {
        this.row = new ArrayList<>();
        return this;
    }

    public InlineKeyboardBuilder button(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();

        button.setCallbackData(callbackData);
        button.setText(text);

        row.add(button);
        return this;
    }

    public InlineKeyboardBuilder button(String text) {
        InlineKeyboardButton button = new InlineKeyboardButton();

        button.setCallbackData(text);
        button.setText(text);

        row.add(button);
        return this;
    }

    public InlineKeyboardBuilder endRow() {
        this.keyboard.add(this.row);
        this.row = null;
        return this;
    }

    public EditMessageText buildEdited() {
        EditMessageText message = new EditMessageText();

        message.setChatId(String.valueOf(chatId));
        message.setMessageId(messageId);
        message.setParseMode("HTML");
        message.setText(text);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        return message;
    }

    public SendMessage buildNew() {
        SendMessage message = new SendMessage();

        message.setChatId(String.valueOf(chatId));
        message.setParseMode("HTML");
        message.setText(text);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        return message;
    }
}
