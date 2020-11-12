package com.example.smarthome.server.telegram;

import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@SuppressWarnings("SpellCheckingInspection")
public class Bot extends TelegramLongPollingBot {

    public static final Logger log = LoggerFactory.getLogger(Bot.class);
    private final static String TOKEN = "1061610133:AAFS9b1Z5GPYNTCqpPVam43xGa4wiph32pE";
    //    private final static String TOKEN = "945155772:AAF6_o_jIz9P-IJnvzUrH99WVpXuTUsyjDo";
//    private final static String USER_NAME = "intelligent_home_beta_bot";
    private final static String USER_NAME = "intelligent_home_bot";
    @Getter
    private static Bot instance;
    private final ExecutorService answerCreatorPool = Executors.newFixedThreadPool(16);
    private final ExecutorService messageExecutorPool = Executors.newFixedThreadPool(16);
    private final Map<Long, UserInstance> instances = new HashMap<>();

    Bot() {
        instance = this;
    }

    private static void answer(UserInstance instance, IncomingMessage msg) {
        instance.sendAnswer(msg);
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.info("------------------------------------- New message incoming -----------------------------------------");
        Runnable task = () -> {
            long ns = System.nanoTime() / 1000;

            long chatId = 0;
            int msgId = 0;
            String callbackId = null;
            String text = null;
            MessageType type = MessageType.UNKNOWN;

            if (update.hasMessage()) {
                if (update.getMessage().hasContact()) {
                    chatId = update.getMessage().getChatId();
                    text = update.getMessage().getContact().getUserID().toString();
                    type = MessageType.CONTACT;
                } else if (update.getMessage().hasLocation()) {
                    Location location = update.getMessage().getLocation();
                    text = location.getLatitude() + " " + location.getLongitude();
                    chatId = update.getMessage().getChatId();
                    type = MessageType.LOCATION;
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

            log.info("Type: " + type.name() + "; " + "Text: " + text + "; " + (callbackId != null ?
                    String.format(" Callback id: %s; Message id: %d", callbackId, msgId) : ""));

            UserInstance instance = getUserInstance(chatId);

            if (!instance.isProcessing()) {
                IncomingMessage msg = new IncomingMessage(msgId, text, callbackId, type);

                long ns_2 = System.nanoTime() / 1000;

                answer(instance, msg);

                long ns_3 = System.nanoTime() / 1000;
                log.info("Prepared in " + (ns_2 - ns) + " mcs" + "; after answer " + (ns_3 - ns_2) + "mcs");
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

        answerCreatorPool.execute(task);
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
            GetChat getChatMethod = new GetChat();
            getChatMethod.setChatId(String.valueOf(userId));
            Chat chat = sendApiMethod(getChatMethod);
            String firstName = chat.getFirstName();
            String lastName = chat.getLastName();
            return firstName + (lastName != null ? " " + lastName : "");
        } catch (TelegramApiException e) {
            log.warn(e.getMessage());
        }
        return "";
    }

    public void executeAsync(SendMessage msg, CallbackAction task, Consumer<TelegramApiRequestException> errorHandler) {
        Runnable r = () -> {
            UserInstance user = getUserInstance(Long.parseLong(msg.getChatId()));
            try {
                instance.execute(msg);
                if (task != null) {
                    task.process();
                }
            } catch (TelegramApiRequestException e) {
                e.printStackTrace();
                if (errorHandler != null) {
                    errorHandler.accept(e);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                user.setProcessing(false);
                if (user.getCurrentLvl() != null) {
                    log.info("Current user level is: " + user.getCurrentLvl().getClass().getTypeName());
                }
            }
        };
        executeTask(r);
    }

    public void executeAsync(EditMessageText msg, CallbackAction task, Consumer<TelegramApiRequestException> errorHandler) {
        Runnable r = () -> {
            UserInstance user = getUserInstance(Long.parseLong(msg.getChatId()));
            try {
                instance.execute(msg);
                if (task != null) {
                    task.process();
                }
            } catch (TelegramApiRequestException e) {
                e.printStackTrace();
                if (errorHandler != null) {
                    errorHandler.accept(e);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                user.setProcessing(false);
                if (user.getCurrentLvl() != null) {
                    log.info("Current user level is: " + user.getCurrentLvl().getClass().getTypeName());
                }
            }
        };

        executeTask(r);
    }

    public void executeAsync(DeleteMessage msg, CallbackAction task) {
        Runnable r = () -> {
            UserInstance user = getUserInstance(Long.parseLong(msg.getChatId()));
            try {
                instance.execute(msg);
                if (task != null) {
                    task.process();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                user.setProcessing(false);
                if (user.getCurrentLvl() != null) {
                    log.info("Current user level is: " + user.getCurrentLvl().getClass().getTypeName());
                }
            }
        };

        executeTask(r);
    }

    public void executeAsync(AnswerCallbackQuery callback, CallbackAction task) {
        Runnable r = () -> {
            try {
                instance.execute(callback);
                if (task != null) {
                    task.process();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        executeTask(r);
    }

    private synchronized void executeTask(Runnable task) {
        messageExecutorPool.execute(task);
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
