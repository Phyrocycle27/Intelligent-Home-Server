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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.weather.ListCitiesLevel.goToListCitiesLevel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WeatherLevel implements AnswerCreator {

    @Getter
    private static final WeatherLevel instance = new WeatherLevel();

    private static final WeatherService weather = WeatherService.getInstance();
    private static final Pattern p = Pattern.compile("[_]");

    // ************************************* MESSAGES *************************************************
    private static final String errorGettingWeatherInfo = "Ошибка при получении информаци о погоде";
    private static final String notModified = "Bad Request: message is not modified";
    private static final String buttonInvalid = "Кнопка недействительна";

    @Override
    public boolean create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {
            String[] arr = p.split(msg.getText());
            String cmd = arr[0];

            switch (cmd) {
                case "remove":
                    int id = Integer.parseInt(arr[1]);
                    weather.removeCityForUser(user.getChatId(), id);
                    goToListCitiesLevel(user, msg);
                    EmojiCallback.success(msg.getCallbackId());
                    break;
                case "back":
                    goToListCitiesLevel(user, msg);
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

    public static void goToWeatherLevel(UserInstance user, IncomingMessage msg, int cityId) {
        executeAsync(new InlineKeyboardMessage(user.getChatId(), weather.getWeather(cityId),
                        new ArrayList<CallbackButton>() {{
                            add(new CallbackButton("Удалить", "remove_" + cityId));
                        }})
                        .setMessageId(msg.getId())
                        .hasBackButton(true),
                () -> user.setCurrentLvl(instance));
    }
}
