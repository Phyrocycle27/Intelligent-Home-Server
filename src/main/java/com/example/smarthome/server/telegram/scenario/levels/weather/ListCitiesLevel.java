package com.example.smarthome.server.telegram.scenario.levels.weather;

import com.example.smarthome.server.entity.City;
import com.example.smarthome.server.exceptions.UserNotFoundException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.weather.CityAdditionLevel.goToCityAdditionLevel;
import static com.example.smarthome.server.telegram.scenario.levels.weather.InformationLevel.goToInformationLevel;
import static com.example.smarthome.server.telegram.scenario.levels.weather.WeatherLevel.goToWeatherLevel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ListCitiesLevel implements AnswerCreator {

    @Getter
    private static final ListCitiesLevel instance = new ListCitiesLevel();

    private static final Logger log = LoggerFactory.getLogger(ListCitiesLevel.class);
    private static final WeatherService weather = WeatherService.getInstance();
    private static final Pattern p = Pattern.compile("[_]");

    // ************************************* MESSAGES *************************************************
    private static final String levelMessage = "Выберите один из городов, чтобы узнать в нём текущую погоду, или " +
            "добавьте новый город";
    private static final String buttonInvalid = "Кнопка недействительна";

    @Override
    public boolean create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {
            String[] arr = p.split(msg.getText());
            String cmd = arr[0];

            switch (cmd) {
                case "id":
                    int id = Integer.parseInt(arr[1]);
                    goToWeatherLevel(user, msg, id, () -> EmojiCallback.next(msg.getCallbackId()));
                    break;
                case "add":
                    goToCityAdditionLevel(user, msg, () -> EmojiCallback.next(msg.getCallbackId()));
                    break;
                case "back":
                    goToInformationLevel(user, msg, () -> EmojiCallback.back(msg.getCallbackId()));
                    break;
                default:
                    executeAsync(new AnswerCallback(msg.getCallbackId(), buttonInvalid),
                            () -> user.setProcessing(false));
            }
            // если сообщение успешно обработано, то возвращаем истину
            return true;
        }
        // иначе, если содержание сообщения не может быть обработано уровнем, возвращаем ложь
        return false;
    }

    public static void goToListCitiesLevel(UserInstance user, IncomingMessage msg, CallbackAction action) {
        List<CallbackButton> buttons = new ArrayList<>();
        try {
            List<City> cities = weather.getUserCities(user.getChatId());
            for (City city : cities) {
                CallbackButton btn = new CallbackButton(String.format("%s, %s",
                        city.getName(), city.getState()), "id_" + city.getId());

                if (buttons.contains(btn)) {
                    int index = buttons.indexOf(btn);
                    buttons.get(index).setText(String.format("%s, %s",
                            cities.get(index).getSuburb(), buttons.get(index).getText()));
                    btn.setText(String.format("%s, %s", city.getSuburb(), btn.getText()));
                }

                buttons.add(btn);
            }
        } catch (UserNotFoundException e) {
            log.error(e.getMessage());
        }

        executeAsync(new InlineKeyboardMessage(user.getChatId(), levelMessage, buttons)
                .setMessageId(msg.getId())
                .hasBackButton(true)
                .hasAddButton(true), () -> {
            user.setCurrentLvl(instance);
            if (action != null) action.process();
        });
    }
}
