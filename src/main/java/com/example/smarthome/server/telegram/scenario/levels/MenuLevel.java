package com.example.smarthome.server.telegram.scenario.levels;

import com.example.smarthome.server.service.DeviceAccessService;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;

import java.util.ArrayList;
import java.util.List;

import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.HomeControlLevel.goToHomeControlLevel;
import static com.example.smarthome.server.telegram.scenario.levels.weather.InformationLevel.goToInformationLevel;

public class MenuLevel implements AnswerCreator {

    private static final MenuLevel instance = new MenuLevel();

    private static final DeviceAccessService service = DeviceAccessService.getInstance();

    // ************************************* MESSAGES *************************************************
    private static final String buttonInvalid = "Кнопка недействительна";
    private static final String menuMsg = "Нажмите \"Управление домом\" чтобы перейти к управлению умным домом и " +
            "просмотру информации с датчиков или нажмите \"Информация\", чтобы узнать точное время или погоду";

    // ************************************** BUTTONS *************************************************
    private static final List<CallbackButton> menuButtons = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Управление домом", "home_control"));
        add(new CallbackButton("Информация", "information"));
    }};

    private MenuLevel() {
    }

    public static MenuLevel getInstance() {
        return instance;
    }

    @Override
    public boolean create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {
            switch (msg.getText()) {
                case "home_control":
                    goToHomeControlLevel(user, msg);
                    EmojiCallback.next(msg.getCallbackId());
                    break;
                case "information":
                    goToInformationLevel(user, msg);
                    EmojiCallback.next(msg.getCallbackId());
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

    public static void goToMenuLevel(UserInstance user, IncomingMessage msg) {
        executeAsync(new InlineKeyboardMessage(user.getChatId(), menuMsg, menuButtons)
                .setMessageId(msg.getId())
                .setNumOfColumns(2), () -> user.setCurrentLvl(instance));
    }
}
