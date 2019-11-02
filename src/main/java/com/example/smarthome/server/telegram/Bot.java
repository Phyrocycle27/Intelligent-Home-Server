package com.example.smarthome.server.telegram;

import com.example.smarthome.server.exceptions.UserAlreadyExistsException;
import com.example.smarthome.server.service.DeviceAccessService;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Bot extends TelegramLongPollingBot {

    private final static String TOKEN = "1061610133:AAFS9b1Z5GPYNTCqpPVam43xGa4wiph32pE";
    private final static String USER_NAME = "intelligent_home_bot";
    private static Logger LOGGER;
    private DeviceAccessService service;
    private YandexWeather yandexWeather;

    static {
        LOGGER = Logger.getLogger(Bot.class.getName());
    }

    Bot() {
        init();
    }

    Bot(DefaultBotOptions options) {
        super(options);
        init();
    }

    private void init() {
        service = DeviceAccessService.getInstance();
        yandexWeather = new YandexWeather();
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
            messages = getMsg(text, userId);

        } else messages = new ArrayList<SendMessage>() {{
            add(new SendMessage()
                    .setChatId(update.getMessage().getChatId())
                    .setText("Sorry, I can't read your message because it not contains text")
            );
        }};
        // executing the message
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

    private List<SendMessage> getMsg(String msgText, long userId) {
        List<SendMessage> messages = new ArrayList<>();

        switch (msgText) {
            case "привет":
            case "hello":
            case "test":
            case "тест":
                messages.add(createMsg(userId)
                        .setText("Test message from Norway"));
                break;
            case "time":
            case "время":
                messages.add(createMsg(userId)
                        .setText(String.format("Химкинское время %s",
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))));
                break;
            case "weather":
            case "погода":
                String weather = yandexWeather.getWeather();
                SendMessage msg = createMsg(userId);

                if (weather == null) msg.setText("Ошибка при получении погоды");
                else msg.setText(weather);

                messages.add(msg);
                break;
            case "клавиатура":
                messages.add(getCustomKeyboard(createMsg(userId)));
                break;
            case "токен":
            case "token":
                try {
                    messages.add(createMsg(userId)
                            .setText("Ваш токен успешно сгенерирован!\n" +
                                    "Он требуется для подключения Вашего устройства к серверу\n\n" +
                                    "\u2B07\u2B07\u2B07 ВАШ ТОКЕН \u2B07\u2B07\u2B07"));
                    // Сообщение с токеном
                    messages.add(createMsg(userId)
                            .setText(service.createToken(userId)));

                    messages.add(createMsg(userId)
                            .setText("Пожалуйста, скопируйте и вставьте Ваш токен в соответствующий раздел в " +
                                    "приложении, чтобы Ваше устройство могло подключиться к серверу"));

                } catch (UserAlreadyExistsException e) {
                    LOGGER.log(Level.WARNING, e.getMessage());
                    messages.clear();

                    messages.add(createMsg(userId)
                            .setText("Вам уже выдан токен. Введите его, пожалуйста, в приложении " +
                                    "чтобы Ваше устройство могло подключиться к серверу"));
                }
                break;
            default:
                messages.add(createMsg(userId)
                        .setText(String.format("Вы отправили сообщение с текстом: '%s'", msgText)));
        }
        return messages;
    }

    private SendMessage createMsg(long chatId) {
        return new SendMessage().setChatId(chatId);
    }

    private SendMessage getCustomKeyboard(SendMessage message) {
        message.setText("Custom message text");
        // Create ReplyKeyboardMarkup object
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        // Create the keyboard (list of keyboard rows)
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Create a keyboard row
        KeyboardRow row = new KeyboardRow();
        // Set each button, you can also use KeyboardButton objects if you need something else than text
        row.add("Row 1 Button 1");
        row.add("Row 1 Button 2");
        // Add the first row to the keyboard
        keyboard.add(row);
        // Create another keyboard row
        row = new KeyboardRow();
        // Set each button for the second line
        row.add("Row 2 Button 1");
        row.add("Row 2 Button 2");
        // Add the second row to the keyboard
        keyboard.add(row);

        row = new KeyboardRow();

        row.add("Row 3 Button 1");
        // Add the second row to the keyboard
        keyboard.add(row);
        // Set the keyboard to the markup
        keyboardMarkup.setKeyboard(keyboard);
        // Add it to the message
        message.setReplyMarkup(keyboardMarkup);

        return message;
    }
}
