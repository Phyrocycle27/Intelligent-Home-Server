package com.example.smarthome.server.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bot extends TelegramLongPollingBot {

    //    private final static String TOKEN = "1061610133:AAFS9b1Z5GPYNTCqpPVam43xGa4wiph32pE";
//    private final static String USER_NAME = "intelligent_home_bot";
    private final static String TOKEN = "945155772:AAF6_o_jIz9P-IJnvzUrH99WVpXuTUsyjDo";
    private final static String USER_NAME = "intelligent_home_beta_bot";
    public static final Logger log;
    private static Map<Long, UserInstance> instances;

    static {
        log = LoggerFactory.getLogger(Bot.class);
        instances = new HashMap<>();
    }

    Bot() {
    }

    Bot(DefaultBotOptions options) {
        super(options);
    }

    @Override
    public void onUpdateReceived(Update update) {
        UserInstance.setBot(this);
        log.info("New message incoming");
        Long chatId = 0L;
        int messageId = 0;
        String text = "";

        if (update.hasMessage() && update.getMessage().hasText()) {
            chatId = update.getMessage().getChat().getId();
            text = update.getMessage().getText();
        } else if (update.hasCallbackQuery()) {
            text = update.getCallbackQuery().getData();
            chatId = update.getCallbackQuery().getMessage().getChatId();
        }
        getUserInstance(chatId).sendAnswer(text);
    }

    private UserInstance getUserInstance(long userId) {
        UserInstance userInstance = instances.get(userId);
        if (userInstance == null) {
            userInstance = new UserInstance(userId);
            instances.put(userId, userInstance);
        }
        return userInstance;
    }

    @Override
    public String getBotUsername() {
        return USER_NAME;
    }

    @Override
    public String getBotToken() {
        return TOKEN;
    }
}
