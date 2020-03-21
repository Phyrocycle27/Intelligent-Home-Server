package com.example.smarthome.server.telegram.scenario.levels;

import com.example.smarthome.server.entity.City;
import com.example.smarthome.server.exceptions.UserNotFoundException;
import com.example.smarthome.server.service.WeatherService;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.CityAdditionLevel.goToCityAdditionLevel;
import static com.example.smarthome.server.telegram.scenario.levels.InformationLevel.goToInformationLevel;
import static com.example.smarthome.server.telegram.scenario.levels.WeatherLevel.goToWeatherLevel;

public class ListCitiesLevel implements AnswerCreator {

    private static final ListCitiesLevel instance = new ListCitiesLevel();
    private static final WeatherService weather = WeatherService.getInstance();

    private static final Logger log = LoggerFactory.getLogger(ListCitiesLevel.class);
    private static final Pattern p = Pattern.compile("[_]");

    // ************************************* MESSAGES *************************************************
    private static final String levelMessage = "Выберите один из городов, чтобы узнать в нём текущую погоду, или " +
            "добавьте новый город";

    private ListCitiesLevel() {
    }

    public static ListCitiesLevel getInstance() {
        return instance;
    }

    @Override
    public void create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {
            String[] arr = p.split(msg.getText());
            String cmd = arr[0];

            switch (cmd) {
                case "id":
                    int id = Integer.parseInt(arr[1]);
                    goToWeatherLevel(user, msg, id);
                    EmojiCallback.next(msg.getCallbackId());
                    break;
                case "add":
                    goToCityAdditionLevel(user, msg);
                    EmojiCallback.next(msg.getCallbackId());
                    break;
                case "back":
                    goToInformationLevel(user, msg);
                    EmojiCallback.back(msg.getCallbackId());
                    break;
            }
        }
    }

    public static void goToListCitiesLevel(UserInstance user, IncomingMessage msg) {
        List<CallbackButton> buttons = new ArrayList<>();
        try {
            List<City> cities = weather.getUserCities(user.getChatId());
            for (City city : cities) {
                buttons.add(new CallbackButton(
                        String.format("%s, %s", city.getName(), city.getState()), "id_" + city.getId()
                ));
            }
        } catch (UserNotFoundException e) {
            log.error(e.getMessage());
        }

        executeAsync(new InlineKeyboardMessage(user.getChatId(), levelMessage, buttons)
                        .setMessageId(msg.getId())
                        .hasBackButton(true)
                        .hasAddButton(true),
                () -> user.setCurrentLvl(instance));
    }
}
