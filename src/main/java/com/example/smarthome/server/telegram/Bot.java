package com.example.smarthome.server.telegram;

import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;
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

    //    private final static String TOKEN = "1061610133:AAFS9b1Z5GPYNTCqpPVam43xGa4wiph32pE";
    //    private final static String USER_NAME = "intelligent_home_bot";
    private final static String TOKEN = "945155772:AAF6_o_jIz9P-IJnvzUrH99WVpXuTUsyjDo";
    private final static String USER_NAME = "intelligent_home_beta_bot";

    public static final Logger log = LoggerFactory.getLogger(Bot.class);

    private Map<Long, UserInstance> instances = new HashMap<>();
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
        log.info("------------------------------------- New message incoming -----------------------------------------");
        long one = System.nanoTime() / 1000;

        Runnable task = () -> {
            log.info("Processing is starting...");

            long ns = System.nanoTime() / 1000;

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

            log.info("Text: " + text + "; " + (callbackId != null ?
                    String.format(" Callback id: %s; Message id: %d", callbackId, msgId) : ""));

            UserInstance instance = getUserInstance(chatId);

            if (!instance.isProcessing()) {
                IncomingMessage msg = new IncomingMessage(msgId, text, callbackId, type);

                long ns_2 = System.nanoTime() / 1000;

                answer(instance, msg);

                long ns_3 = System.nanoTime() / 1000;
                log.info("Prepared in " + (ns_2 - ns) + " mcs" + "; after answer " + (ns_3 - ns_2) + "mcs");

                if (instance.getCurrentLvl() != null) {
                    log.info("Current user level is: " + instance.getCurrentLvl().getClass().toString());
                }
            } else {
                if (type == MessageType.CALLBACK) {
                    if (instance.getSpamCount() < 5) {
                        MessageExecutor.executeAsync(new AnswerCallback(callbackId, "Пожалуйста, подождите"));
                        instance.spam();
                    } else {
                        MessageExecutor.executeAsync(new AnswerCallback(callbackId, "Слишком много сообщений. " +
                                "Своими действиями вы наносите вред друим пользователям. Спам нарушает лицензионное " +
                                "соглашение Telegram LLC и Intelligent Home")
                                .hasAlert(true));
                        instance.clearSpamCount();
                    }
                }
            }
        };
        pool.execute(task);

        long two = System.nanoTime() / 1000;
        log.info("Time " + (two - one));
    }

    private static void answer(UserInstance instance, IncomingMessage msg) {
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

    public String getUserName(long userId) {
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

    public void executeAsync(SendMessage msg, CallbackAction task, Consumer<TelegramApiRequestException> errorHandler) {
        UserInstance instance = getUserInstance(Long.parseLong(msg.getChatId()));

        sendApiMethodAsync(msg, new SentCallback<Message>() {
            @Override
            public void onResult(BotApiMethod<Message> botApiMethod, Message message) {
                if (task != null) {
                    task.process();
                }
                instance.setProcessing(false);
            }

            @Override
            public void onError(BotApiMethod<Message> botApiMethod, TelegramApiRequestException e) {
                if (errorHandler != null) {
                    errorHandler.accept(e);
                }
                instance.setProcessing(false);
            }

            @Override
            public void onException(BotApiMethod<Message> botApiMethod, Exception e) {
                instance.setProcessing(false);
            }
        });
    }

    public void executeAsync(EditMessageText msg, CallbackAction task, Consumer<TelegramApiRequestException> errorHandler) {
        UserInstance instance = getUserInstance(Long.parseLong(msg.getChatId()));

        sendApiMethodAsync(msg, new SentCallback<Serializable>() {
            @Override
            public void onResult(BotApiMethod<Serializable> botApiMethod, Serializable serializable) {
                if (task != null) {
                    task.process();
                }
                instance.setProcessing(false);
            }

            @Override
            public void onError(BotApiMethod<Serializable> botApiMethod, TelegramApiRequestException e) {
                if (errorHandler != null) {
                    errorHandler.accept(e);
                }
                instance.setProcessing(false);
            }

            @Override
            public void onException(BotApiMethod<Serializable> botApiMethod, Exception e) {
                instance.setProcessing(false);
            }
        });
    }

    public void executeAsync(AnswerCallbackQuery callbackQuery, CallbackAction task) {
        sendApiMethodAsync(callbackQuery, new SentCallback<Boolean>() {
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

    public void executeAsync(DeleteMessage msg, CallbackAction task) {
        UserInstance instance = getUserInstance(Long.parseLong(msg.getChatId()));

        sendApiMethodAsync(msg, new SentCallback<Boolean>() {
            @Override
            public void onResult(BotApiMethod<Boolean> botApiMethod, Boolean aBoolean) {
                if (task != null) {
                    task.process();
                }
                instance.setProcessing(false);
            }

            @Override
            public void onError(BotApiMethod<Boolean> botApiMethod, TelegramApiRequestException e) {
                instance.setProcessing(false);
            }

            @Override
            public void onException(BotApiMethod<Boolean> botApiMethod, Exception e) {
                instance.setProcessing(false);
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
