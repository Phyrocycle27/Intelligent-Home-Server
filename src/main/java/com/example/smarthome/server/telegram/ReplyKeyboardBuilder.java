package com.example.smarthome.server.telegram;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class ReplyKeyboardBuilder {
    private long chatId;
    private String text;
    private boolean oneTimeKeyboard;

    private List<KeyboardRow> keyboard = new ArrayList<>();
    private KeyboardRow row = new KeyboardRow();

    private ReplyKeyboardBuilder() {
    }

    public static ReplyKeyboardBuilder create(long chatId) {
        ReplyKeyboardBuilder builder = new ReplyKeyboardBuilder();
        builder.chatId = chatId;
        return builder;
    }

    public ReplyKeyboardBuilder setText(String text) {
        this.text = text;
        return this;
    }

    public ReplyKeyboardBuilder hasOneTimeKeyboard(boolean b) {
        this.oneTimeKeyboard = b;
        return this;
    }

    public ReplyKeyboardBuilder row() {
        this.row = new KeyboardRow();
        return this;
    }

    public ReplyKeyboardBuilder button(String text) {
        this.row.add(text);
        return this;
    }

    public ReplyKeyboardBuilder endRow() {
        this.keyboard.add(this.row);
        this.row = null;
        return this;
    }

    public SendMessage build() {
        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText(text)
                .setParseMode("HTML");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup()
                .setResizeKeyboard(true)
                .setOneTimeKeyboard(oneTimeKeyboard)
                .setSelective(true);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        return message;
    }
}
