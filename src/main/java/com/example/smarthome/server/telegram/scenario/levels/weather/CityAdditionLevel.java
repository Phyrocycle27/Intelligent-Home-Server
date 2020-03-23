package com.example.smarthome.server.telegram.scenario.levels.weather;

import com.example.smarthome.server.service.WeatherService;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;

import java.util.regex.Pattern;

import static com.example.smarthome.server.telegram.MessageExecutor.deleteAsync;
import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.weather.ListCitiesLevel.goToListCitiesLevel;

public class CityAdditionLevel implements AnswerCreator {

    private static final CityAdditionLevel instance = new CityAdditionLevel();

    private static final WeatherService weather = WeatherService.getInstance();
    private static final Pattern p = Pattern.compile("[ ]");

    // ************************************* MESSAGES *************************************************
    private static final String sendLocation = "Отправьте геопозицию, чтобы отслеживать погоду на этой точке";
    private static final String cityNotFound = "Похоже, что в этом районе нет метеостанций OpenWeatherMap. " +
            "Попробуйте отправить другую геопозицию";
    private static final String buttonInvalid = "Кнопка недействительна";

    private CityAdditionLevel() {
    }

    public static CityAdditionLevel getInstance() {
        return instance;
    }

    @Override
    public boolean create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {
            if (msg.getText().equals("back")) {
                goToListCitiesLevel(user, msg);
                EmojiCallback.back(msg.getCallbackId());
            } else {
                executeAsync(new AnswerCallback(msg.getCallbackId(), buttonInvalid));
            }
            // если сообщение успешно обработано, то возвращаем истину
            return true;
        } else if (msg.getType() == MessageType.LOCATION) {
            deleteAsync(user.getChatId(), user.getLastMessageId(), null);
            String[] coordinates = p.split(msg.getText());
            float lat = Float.parseFloat(coordinates[0]);
            float lon = Float.parseFloat(coordinates[1]);
            try {
                weather.addCity(user.getChatId(), lat, lon);
                goToListCitiesLevel(user, msg);
            } catch (Exception e) {
                e.printStackTrace();
                executeAsync(new InlineKeyboardMessage(user.getChatId(), cityNotFound, null)
                        .hasBackButton(true), null);
            }
            // если сообщение успешно обработано, то возвращаем истину
            return true;
        }
        // иначе, если содержание сообщения не может быть обработано уровнем, возвращаем ложь
        return false;
    }

    public static void goToCityAdditionLevel(UserInstance user, IncomingMessage msg) {
        executeAsync(new InlineKeyboardMessage(user.getChatId(), sendLocation, null)
                .setMessageId(msg.getId())
                .hasBackButton(true), () -> {
            user.setCurrentLvl(instance);
            user.setLastMessageId(msg.getId());
        });
    }
}
