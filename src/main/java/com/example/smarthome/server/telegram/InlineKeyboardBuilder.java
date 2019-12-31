package com.example.smarthome.server.telegram;

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

    private List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    private List<InlineKeyboardButton> row = null;

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
        row.add(new InlineKeyboardButton().setText(text).setCallbackData(callbackData));
        return this;
    }

    public InlineKeyboardBuilder endRow() {
        this.keyboard.add(this.row);
        this.row = null;
        return this;
    }

    public EditMessageText buildEdited() {
        EditMessageText message = new EditMessageText();

        message.setChatId(chatId);
        message.setMessageId(messageId);
        message.setText(text);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        return message;
    }

    public SendMessage buildNew() {
        SendMessage message = new SendMessage();

        message.setChatId(chatId);
        message.setText(text);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        return message;
    }

}
