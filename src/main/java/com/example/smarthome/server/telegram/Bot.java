package com.example.smarthome.server.telegram;

import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.updateshandlers.SentCallback;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Bot extends TelegramLongPollingBot {

    private final static String TOKEN = "1061610133:AAFS9b1Z5GPYNTCqpPVam43xGa4wiph32pE";
    private final static String USER_NAME = "intelligent_home_bot";
    //    private final static String TOKEN = "945155772:AAF6_o_jIz9P-IJnvzUrH99WVpXuTUsyjDo";
//    private final static String USER_NAME = "intelligent_home_beta_bot";
    public static final Logger log = LoggerFactory.getLogger(Bot.class);
    private static Map<Long, UserInstance> instances = new HashMap<>();
    private ExecutorService pool = Executors.newFixedThreadPool(16);

    private static Bot instance;

    private Bot(DefaultBotOptions options) {
        super(options);
    }

    private Bot() {
    }

    public static Bot getInstance(DefaultBotOptions options) {
        if (instance == null) {
            if (options != null) {
                instance = new Bot(options);
            } else instance = new Bot();
        }
        return instance;
    }

    public static Bot getInstance() {
        return getInstance(null);
    }

    @Override
    public void onUpdateReceived(Update update) {
//        long one = System.nanoTime() / 1000;
        Runnable task = () -> {
//            long ns = System.nanoTime() / 1000;
//            log.info("New message incoming");
            long chatId = 0;
            int msgId = 0;
            String callbackId = null;
            String text = null;
            MessageType type = null;

            if (update.hasMessage()) {
                if (update.getMessage().hasContact()) {
                    chatId = update.getMessage().getChatId();
                    text = update.getMessage().getContact().getUserID().toString();
                    type = MessageType.CONTACT;
                } else if (update.getMessage().hasText()) {
                    chatId = update.getMessage().getChatId();
                    text = update.getMessage().getText();
                    type = MessageType.TEXT;
                }
            } else if (update.hasCallbackQuery()) {
                text = update.getCallbackQuery().getData();
                chatId = update.getCallbackQuery().getMessage().getChatId();
                msgId = update.getCallbackQuery().getMessage().getMessageId();
                callbackId = update.getCallbackQuery().getId();
                type = MessageType.CALLBACK;
            }

//            log.info("Text: " + text + (callbackId != null ?
//                    String.format(" Callback id: %s Message id: %d", callbackId, msgId) : ""));

//            long ns_2 = System.nanoTime() / 1000;
            UserInstance instance = getUserInstance(chatId);
            IncomingMessage msg = new IncomingMessage(msgId, text, callbackId, type);

            answer(instance, msg);
//            long ns_3 = System.nanoTime() / 1000;
//            log.info("Prepared in " + (ns_2 - ns) + " mcs" + "; after answer " + (ns_3 - ns_2) + "mcs");
        };
        pool.execute(task);

//        long two = System.nanoTime() / 1000;
//        log.info("Time " + (two - one));
    }

    private synchronized void answer(UserInstance instance, IncomingMessage msg) {
        instance.sendAnswer(msg);
    }

    private synchronized UserInstance getUserInstance(long userId) {
        UserInstance userInstance = instances.get(userId);
        if (userInstance == null) {
            userInstance = new UserInstance(userId);
            instances.put(userId, userInstance);
        }
        return userInstance;
    }

    public synchronized String getUserName(long userId) {
        try {
            Chat chat = sendApiMethod(new GetChat().setChatId(userId));
            String firstName = chat.getFirstName();
            String lastName = chat.getLastName();
            return firstName + (lastName != null ? " " + lastName : "");
        } catch (TelegramApiException e) {
            log.warn(e.getMessage());
        }
        return "";
    }

    public synchronized void executeAsync(SendMessage msg, CallbackAction task,
                                          Consumer<TelegramApiRequestException> errorHandler) {

        sendApiMethodAsync(msg, new SentCallback<Message>() {
            @Override
            public void onResult(BotApiMethod<Message> botApiMethod, Message message) {
                if (task != null) {
                    task.process();
                }
            }

            @Override
            public void onError(BotApiMethod<Message> botApiMethod, TelegramApiRequestException e) {
                if (errorHandler != null) {
                    errorHandler.accept(e);
                }
            }

            @Override
            public void onException(BotApiMethod<Message> botApiMethod, Exception e) {

            }
        });
    }

    public synchronized void executeAsync(EditMessageText msg, CallbackAction task,
                                          Consumer<TelegramApiRequestException> errorHandler) {

        sendApiMethodAsync(msg, new SentCallback<Serializable>() {
            @Override
            public void onResult(BotApiMethod<Serializable> botApiMethod, Serializable serializable) {
                if (task != null) {
                    task.process();
                }
            }

            @Override
            public void onError(BotApiMethod<Serializable> botApiMethod, TelegramApiRequestException e) {
                if (errorHandler != null) {
                    errorHandler.accept(e);
                }
            }

            @Override
            public void onException(BotApiMethod<Serializable> botApiMethod, Exception e) {

            }
        });
    }

    public synchronized void executeAsync(AnswerCallbackQuery callbackQuery) {
        sendApiMethodAsync(callbackQuery, new SentCallback<Boolean>() {
            @Override
            public void onResult(BotApiMethod<Boolean> botApiMethod, Boolean aBoolean) {

            }

            @Override
            public void onError(BotApiMethod<Boolean> botApiMethod, TelegramApiRequestException e) {

            }

            @Override
            public void onException(BotApiMethod<Boolean> botApiMethod, Exception e) {

            }
        });
    }

    public synchronized void executeAsync(DeleteMessage msg, CallbackAction task) {
        sendApiMethodAsync(msg, new SentCallback<Boolean>() {
            @Override
            public void onResult(BotApiMethod<Boolean> botApiMethod, Boolean aBoolean) {
                if (task != null) {
                    task.process();
                }
            }

            @Override
            public void onError(BotApiMethod<Boolean> botApiMethod, TelegramApiRequestException e) {

            }

            @Override
            public void onException(BotApiMethod<Boolean> botApiMethod, Exception e) {

            }
        });
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
