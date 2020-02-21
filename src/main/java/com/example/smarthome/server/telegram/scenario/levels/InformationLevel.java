package com.example.smarthome.server.telegram.scenario.levels;

import com.example.smarthome.server.telegram.Bot;
import com.example.smarthome.server.telegram.MessageExecutor;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.Weather;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class InformationLevel implements AnswerCreator {

    private static final InformationLevel instance = new InformationLevel();

    private static final Logger log = LoggerFactory.getLogger(InformationLevel.class.getName());
    private static final Weather weatherService = Weather.getInstance();
    private static final Bot bot = Bot.getInstance();

    // ************************************* MESSAGES *************************************************
    private static final String infoMsg = "Выберите \"Погода\" чтобы узнать погоду в совём городе " +
            "или нажмите \"Время\"чтобы узнать точное время в вашем городе";
    private static final String buttonInvalid = "Кнопка недействительна";
    private static final String errorGettingWeatherInfo = "Ошибка при получении информаци о погоде";

    // ************************************** BUTTONS *************************************************
    private static final List<CallbackButton> infoButtons = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Погода", "weather"));
        add(new CallbackButton("Время", "time"));
    }};

    private InformationLevel() {
    }

    public static InformationLevel getInstance() {
        return instance;
    }

    @Override
    public void create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK)
            switch (msg.getText()) {
                case "weather":
                    String weather = weatherService.getWeather();

                    try {
                        updateInformationMessage(user, msg, weather != null ? weather : errorGettingWeatherInfo);
                    } catch (RuntimeException e) {
                        log.error(e.getMessage());
                        MessageExecutor.execute(bot, new AnswerCallback(msg.getCallbackId(), "\u2705"));
                    }
                    break;
                case "time":
                    try {
                        updateInformationMessage(user, msg, String.format("Химкинское время %s",
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
                    } catch (RuntimeException e) {
                        log.error(e.getMessage());
                        MessageExecutor.execute(bot, new AnswerCallback(msg.getCallbackId(), "\u2705"));
                    }
                    break;
                case "back":
                    MenuLevel.goToMenuLevel(user, msg);
                    break;
                default:
                    if (!msg.getCallbackId().isEmpty())
                        MessageExecutor.execute(bot, new AnswerCallback(msg.getCallbackId(), buttonInvalid));
            }
    }

    public static void updateInformationMessage(UserInstance user, IncomingMessage msg, String s) {
        MessageExecutor.execute(bot, new InlineKeyboardMessage(user.getChatId(), s, infoButtons)
                .setMessageId(msg.getId())
                .setNumOfColumns(2)
                .hasBackButton(true));
    }

    public static void goToInformationLevel(UserInstance user, IncomingMessage msg) {
        MessageExecutor.execute(bot, new InlineKeyboardMessage(user.getChatId(), infoMsg, infoButtons)
                .setMessageId(msg.getId())
                .setNumOfColumns(2)
                .hasBackButton(true));

        user.setCurrentLvl(instance);
    }
}
