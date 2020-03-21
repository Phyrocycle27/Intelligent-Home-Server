package com.example.smarthome.server.telegram.scenario.levels;

import com.example.smarthome.server.service.WeatherService;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.ListCitiesLevel.goToListCitiesLevel;

public class WeatherLevel implements AnswerCreator {

    private static final WeatherLevel instance = new WeatherLevel();

    private static final WeatherService weather = WeatherService.getInstance();
    private static final Pattern p = Pattern.compile("[_]");

    // ************************************* MESSAGES *************************************************
    private static final String errorGettingWeatherInfo = "Ошибка при получении информаци о погоде";
    private static final String notModified = "Bad Request: message is not modified";

    private WeatherLevel() {
    }

    public static WeatherLevel getInstance() {
        return instance;
    }

    @Override
    public void create(UserInstance user, IncomingMessage msg) {
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
            }
        }
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
