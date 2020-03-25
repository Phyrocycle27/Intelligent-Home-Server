package com.example.smarthome.server.telegram.scenario.levels.weather;

import com.example.smarthome.server.service.WeatherService;
import com.example.smarthome.server.telegram.CallbackAction;
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
            int cityId;

            switch (cmd) {
                case "remove":
                    cityId = Integer.parseInt(arr[1]);
                    weather.removeCityForUser(user.getChatId(), cityId);
                    goToListCitiesLevel(user, msg);
                    EmojiCallback.success(msg.getCallbackId());
                    break;
                case "back":
                    goToListCitiesLevel(user, msg);
                    EmojiCallback.back(msg.getCallbackId());
                    break;
                case "forecast":
                    int forecast = Integer.parseInt(arr[1]);
                    cityId = Integer.parseInt(arr[2]);
                    String s = weather.getForecast(cityId, forecast);
                    updateMessage(user, msg, s, cityId, forecast,
                            () -> EmojiCallback.success(msg.getCallbackId()));
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

    private static void updateMessage(UserInstance user, IncomingMessage msg, String s, int cityId,
                                      int currForecast, CallbackAction task) {

        executeAsync(new InlineKeyboardMessage(user.getChatId(), s,
                new ArrayList<CallbackButton>() {{
                    if (currForecast != 3) {
                        add(new CallbackButton("Прогноз на 3 часа", "forecast_3_" + cityId));
                    }
                    if (currForecast != 6) {
                        add(new CallbackButton("Прогноз на 6 часов", "forecast_6_" + cityId));
                    }
                    if (currForecast != 12) {
                        add(new CallbackButton("Прогноз на 12 часов", "forecast_12_" + cityId));
                    }
                    if (currForecast != 24) {
                        add(new CallbackButton("Прогноз на сутки", "forecast_24_" + cityId));
                    }
                    add(new CallbackButton("Удалить", "remove_" + cityId));
                }})
                .hasBackButton(true)
                .setMessageId(msg.getId()), task);
    }

    public static void goToWeatherLevel(UserInstance user, IncomingMessage msg, int cityId) {
        String s = weather.getCurrent(cityId);
        updateMessage(user, msg, s, cityId, 0, () -> user.setCurrentLvl(instance));
    }
}
