package com.example.smarthome.server.telegram;

import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.updateshandlers.SentCallback;

import java.util.HashMap;
import java.util.Map;

public class Bot extends TelegramLongPollingBot {
        private final static String TOKEN = "1061610133:AAFS9b1Z5GPYNTCqpPVam43xGa4wiph32pE";
        private final static String USER_NAME = "intelligent_home_bot";
//    private final static String TOKEN = "945155772:AAF6_o_jIz9P-IJnvzUrH99WVpXuTUsyjDo";
//    private final static String USER_NAME = "intelligent_home_beta_bot";
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
        long chatId = 0;
        int msgId = 0;
        String callbackId = null;
        String text = null;
        MessageType type = MessageType.TEXT;


        if (update.hasMessage()) {
            if (update.getMessage().hasContact()) {
                chatId = update.getMessage().getChatId();
                text = update.getMessage().getContact().getUserID().toString();
                type = MessageType.CONTACT;
            } else if (update.getMessage().hasText()) {
                chatId = update.getMessage().getChatId();
                text = update.getMessage().getText();
            }
        } else if (update.hasCallbackQuery()) {
            text = update.getCallbackQuery().getData();
            chatId = update.getCallbackQuery().getMessage().getChatId();
            msgId = update.getCallbackQuery().getMessage().getMessageId();
            callbackId = update.getCallbackQuery().getId();
            type = MessageType.CALLBACK;
        }

        log.info("Text: " + text + (callbackId != null ?
                String.format(" Callback id: %s Message id: %d", callbackId, msgId) : ""));

        UserInstance instance = getUserInstance(chatId);
        IncomingMessage msg = new IncomingMessage(msgId, text, callbackId, type);
        answer(instance, msg);
    }

    private void answer(UserInstance instance, IncomingMessage msg) {
        instance.sendAnswer(msg);
    }

    private UserInstance getUserInstance(long userId) {
        UserInstance userInstance = instances.get(userId);
        if (userInstance == null) {
            userInstance = new UserInstance(userId);
            instances.put(userId, userInstance);
        }
        return userInstance;
    }

    public String getUsername(long userId) {
        try {
            Chat chat = sendApiMethod(new GetChat().setChatId(userId));
            String firstName = chat.getFirstName();
            String lastName = chat.getLastName();
            return firstName + (lastName != null? " " + lastName: "");
        } catch (TelegramApiException e) {
            log.warn(e.getMessage());
        }
        return "";
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
