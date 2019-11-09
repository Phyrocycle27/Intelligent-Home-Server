package com.example.smarthome.server.telegram;

import com.example.smarthome.server.service.DeviceAccessService;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Bot extends TelegramLongPollingBot {

    private final static String TOKEN = "1061610133:AAFS9b1Z5GPYNTCqpPVam43xGa4wiph32pE";
    private final static String USER_NAME = "intelligent_home_bot";
    private static final DeviceAccessService service;
    private static Logger LOGGER;
    private static Map<Long, UserInstance> userInstances;

    static {
        LOGGER = Logger.getLogger(Bot.class.getName());
        service = DeviceAccessService.getInstance();
        userInstances = new HashMap<>();
    }

    Bot() {
    }

    Bot(DefaultBotOptions options) {
        super(options);
    }


    @Override
    public void onUpdateReceived(Update update) {
        List<SendMessage> messages;

        if (update.hasMessage() && update.getMessage().hasText()) {

            String text = update.getMessage().getText().toLowerCase();
            String userName = update.getMessage().getChat().getFirstName() +
                    " " + update.getMessage().getChat().getLastName();
            Long userId = update.getMessage().getChat().getId();

            LOGGER.log(Level.INFO, String.format("New message '%s' from %s (id %d)", text, userName, userId));
            // устанавливаем действие, отображаемое у пользователя
            SendChatAction sendChatAction = new SendChatAction()
                    .setChatId(update.getMessage().getChatId())
                    .setAction(ActionType.TYPING);
            // executing the action
            try {
                execute(sendChatAction);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            // preparing the answering message
            UserInstance userInstance = userInstances.get(userId);
            if (userInstance == null) {
                userInstance = new UserInstance(userId);
                userInstances.put(userId, userInstance);
            }
            messages = userInstance.getMessage(text);

        } else messages = new ArrayList<SendMessage>() {{
            add(new SendMessage()
                    .setChatId(update.getMessage().getChatId())
                    .setText("Извините, я не могу прочитать ваше собщение, потому что оно не содержит текст")
            );
        }};
        // sending the message
        try {
            for (SendMessage message : messages) {
                execute(message);
            }
            messages.clear();
        } catch (TelegramApiException ex) {
            ex.printStackTrace();
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
