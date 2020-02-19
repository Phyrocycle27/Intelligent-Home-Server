package com.example.smarthome.server.telegram.scenario.levels;

import com.example.smarthome.server.service.DeviceAccessService;
import com.example.smarthome.server.telegram.Bot;
import com.example.smarthome.server.telegram.MessageExecutor;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;

import java.util.ArrayList;
import java.util.List;

public class MenuLevel implements AnswerCreator {

    private static final MenuLevel instance = new MenuLevel();

    private static final DeviceAccessService service = DeviceAccessService.getInstance();
    private static final Bot bot = Bot.getInstance();

    // ************************************* MESSAGES ************************************************
    private static final String tokenNotFound = "Похоже, что у Вас нет токена...\nЧтобы управлять домом через этого телеграм " +
            "бота Вам нужен уникальный токен, который Ваша Raspberry PI будет использовать для подключения у серверу";
    private static final String infoMsg = "Выберите \"Погода\" чтобы узнать погоду в совём городе " +
            "или нажмите \"Время\"чтобы узнать точное время в вашем городе";
    private static final String buttonInvalid = "Кнопка недействительна";

    // ************************************** BUTTONS *************************************************
    private static final List<CallbackButton> tokenGenButton = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Сгенерировать токен", "token_gen"));
    }};
    private static final List<CallbackButton> infoButtons = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Погода", "weather"));
        add(new CallbackButton("Время", "time"));
    }};

    private MenuLevel() {
    }

    public static MenuLevel getInstance() {
        return instance;
    }

    @Override
    public void create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK)
            switch (msg.getText()) {
                case "home_control":
                    if (service.isExists(user.getChatId())) {
                        // goToHomeControlLevel(msg);
                    } else {
                        MessageExecutor.execute(bot, new InlineKeyboardMessage(user.getChatId(), tokenNotFound,
                                tokenGenButton)
                                .setMessageId(msg.getId())
                                .hasBackButton(true));
                        user.setCurrentLvl(HomeControlLevel.getInstance());
                    }
                    break;
                case "information":
                    MessageExecutor.execute(bot, new InlineKeyboardMessage(user.getChatId(), infoMsg, infoButtons)
                            .setMessageId(msg.getId())
                            .setNumOfColumns(2)
                            .hasBackButton(true));
                    user.setCurrentLvl(InformationLevel.getInstance());
                    break;
                default:
                    if (!msg.getCallbackId().isEmpty())
                        MessageExecutor.execute(bot, new AnswerCallback(msg.getCallbackId(), buttonInvalid));
            }
    }

    public void goToMain(UserInstance user, IncomingMessage msg) {

    }
}
