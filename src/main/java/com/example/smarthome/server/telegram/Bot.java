package com.example.smarthome.server.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
        Long userId = update.getMessage().getChat().getId();
        List<SendMessage> messages;

        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText().toLowerCase();
            /*
            // устанавливаем действие, отображаемое у пользователя
            SendChatAction sendChatAction = new SendChatAction()
                    .setChatId(update.getMessage().getChatId())
                    .setAction(ActionType.TYPING);

            // executing the action
            try {
                execute(sendChatAction);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }*/

            new Thread(new MessageCreator(text, userId)).start();

        } else {
            messages = new ArrayList<SendMessage>() {{
                add(new SendMessage()
                        .setChatId(userId)
                        .setText("Извините, я не могу распознать ваше собщение, потому что оно не содержит текст")
                );
            }};
            execute(messages);
        }
    }

    private synchronized UserInstance getUserInstance(long userId) {
        UserInstance userInstance = instances.get(userId);
        if (userInstance == null) {
            userInstance = new UserInstance(userId);
            instances.put(userId, userInstance);
        }
        return userInstance;
    }

    private synchronized void execute(List<SendMessage> messages) {
        try {
            for (SendMessage message : messages)
                execute(message);
            messages.clear();
        } catch (TelegramApiException ex) {
            ex.printStackTrace();
        }
    }

    class MessageCreator implements Runnable {

        private final String incomingText;
        private final long userId;

        public MessageCreator(String incomingText, long userId) {
            this.incomingText = incomingText;
            this.userId = userId;
        }

        @Override
        public void run() {
            UserInstance userInstance = getUserInstance(userId);
            List<SendMessage> msg = userInstance.getMessage(incomingText);
            execute(msg);
        }
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
