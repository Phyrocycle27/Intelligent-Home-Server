package com.example.smarthome.server.telegram.scenario.levels.home_control;

import com.example.smarthome.server.service.DeviceAccessService;
import com.example.smarthome.server.telegram.Bot;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.Message;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;
import com.example.smarthome.server.telegram.scenario.levels.MenuLevel;

import java.util.ArrayList;
import java.util.List;

import static com.example.smarthome.server.telegram.MessageExecutor.execute;
import static com.example.smarthome.server.telegram.scenario.levels.MenuLevel.goToMenuLevel;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UsersLevel.goToUsersLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.DevicesLevel.goToDevicesLevel;

public class HomeControlLevel implements AnswerCreator {

    private static final HomeControlLevel instance = new HomeControlLevel();

    private static final DeviceAccessService service = DeviceAccessService.getInstance();
    private static final Bot bot = Bot.getInstance();

    // ************************************* MESSAGES ************************************************
    private static final String buttonInvalid = "Кнопка недействительна";
    private static final String tokenSuccessGen = "Ваш токен успешно сгенерирован!\nОн требуется для подключения Вашей " +
            "Raspberry PI к серверу.\nПожалуйста, скопируйте и вставьте Ваш токен в соответствующий раздел в приложении." +
            "\n\n\u21E7\u21E7\u21E7 ВАШ ТОКЕН \u21E7\u21E7\u21E7";
    private static final String tokenNotFound = "Похоже, что у Вас нет токена...\nЧтобы управлять домом через этого телеграм " +
            "бота Вам нужен уникальный токен, который Ваша Raspberry PI будет использовать для подключения у серверу";
    private static final String channelNotFound = "Ваша Raspberry PI не подключена к серверу\n" +
            "Введите, пожалуйста, свой токен в соответствующем разделе в приложении чтобы Ваше устройство могло " +
            "подключиться к серверу";
    private static final String homeControl = "Выберите Устройства, чтобы управлять устройствами или добавить новое, или " +
            "выберите Датчики чтобы посмотреть показания или добавить новый датчик";

    // ************************************** BUTTONS *************************************************
    private static final List<CallbackButton> homeControlButtons = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Устройства", "devices"));
        add(new CallbackButton("Датчики", "sensors"));
        add(new CallbackButton("Пользователи", "users"));
    }};
    private static final List<CallbackButton> tokenGenButton = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Сгенерировать токен", "token_gen"));
    }};

    private HomeControlLevel() {
    }

    public static HomeControlLevel getInstance() {
        return instance;
    }

    @Override
    public void create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK)
            switch (msg.getText()) {
                case "devices":
                    goToDevicesLevel(user, msg);
                    break;
                case "users":
                    goToUsersLevel(user, msg);
                    break;
                case "token_gen":
                    sendToken(user, msg);
                    goToMenuLevel(user, msg);
                    break;
                case "back":
                    goToMenuLevel(user, msg);
                    break;
                default:
                    execute(bot, new AnswerCallback(msg.getCallbackId(), buttonInvalid));
            }
    }

    public static void goToHomeControlLevel(UserInstance user, IncomingMessage msg) {
        if (service.isExists(user.getChatId())) {
            if (service.isChannelExist(user.getChatId())) {
                execute(bot, new InlineKeyboardMessage(user.getChatId(), homeControl, homeControlButtons)
                        .setNumOfColumns(2)
                        .setMessageId(msg.getId())
                        .hasBackButton(true));

                user.setCurrentLvl(instance);
            } else {
                execute(bot, new AnswerCallback(msg.getCallbackId(), channelNotFound)
                        .hasAlert(true));

                if (user.getCurrentLvl() != MenuLevel.getInstance())
                    goToMenuLevel(user, msg);
            }
        } else {
            execute(bot, new InlineKeyboardMessage(user.getChatId(), tokenNotFound,
                    tokenGenButton)
                    .setMessageId(msg.getId())
                    .hasBackButton(true));

            user.setCurrentLvl(instance);
        }
    }

    private void sendToken(UserInstance user, IncomingMessage msg) {
        execute(bot, new Message(user.getChatId(), tokenSuccessGen)
                .setMessageId(msg.getId()));
        execute(bot, new Message(user.getChatId(),
                service.createToken(user.getChatId())));
    }
}
