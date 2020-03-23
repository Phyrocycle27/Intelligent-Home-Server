package com.example.smarthome.server.telegram.scenario.levels.weather;

import com.example.smarthome.server.service.WeatherService;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
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
import java.util.regex.Pattern;

import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.MenuLevel.goToMenuLevel;
import static com.example.smarthome.server.telegram.scenario.levels.weather.ListCitiesLevel.goToListCitiesLevel;

public class InformationLevel implements AnswerCreator {

    private static final InformationLevel instance = new InformationLevel();

    private static final Logger log = LoggerFactory.getLogger(InformationLevel.class.getName());
    private static final WeatherService weatherService = WeatherService.getInstance();

    private static final Pattern p = Pattern.compile("[_]");

    // ************************************* MESSAGES *************************************************
    private static final String infoMsg = "Выберите \"Погода\" чтобы узнать погоду в совём городе " +
            "или нажмите \"Время\"чтобы узнать точное время в вашем городе";
    private static final String buttonInvalid = "Кнопка недействительна";
    private static final String notModified = "Bad Request: message is not modified";

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
    public boolean create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {
            switch (msg.getText()) {
                case "weather":
                    goToListCitiesLevel(user, msg);
                    EmojiCallback.next(msg.getCallbackId());
                    break;
                case "time":
                    updateInformationMessage(user, msg, String.format("Химкинское время %s",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
                    break;
                case "back":
                    goToMenuLevel(user, msg);
                    EmojiCallback.back(msg.getCallbackId());
                    break;
                default:
                    executeAsync(new AnswerCallback(msg.getCallbackId(), buttonInvalid));
            }
            // если сообщение успешно обработано, то возвращаем истину
            return true;
        }
        // иначе, если содержание сообщения не может быть обработано уровнем, возвращаем ложь
        return false;
    }

    public static void updateInformationMessage(UserInstance user, IncomingMessage msg, String s) {
        executeAsync(new InlineKeyboardMessage(user.getChatId(), s, infoButtons)
                        .setMessageId(msg.getId())
                        .setNumOfColumns(2)
                        .hasBackButton(true),
                () -> EmojiCallback.success(msg.getCallbackId()),
                e -> {
                    if (e.getApiResponse().startsWith(notModified)) {
                        EmojiCallback.success(msg.getCallbackId());
                    }
                });
    }

    public static void goToInformationLevel(UserInstance user, IncomingMessage msg) {
        executeAsync(new InlineKeyboardMessage(user.getChatId(), infoMsg, infoButtons)
                .setMessageId(msg.getId())
                .setNumOfColumns(2)
                .hasBackButton(true), () -> user.setCurrentLvl(instance));
    }
}
