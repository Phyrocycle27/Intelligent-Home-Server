package com.example.smarthome.server.telegram.scenario.levels;

import com.example.smarthome.server.service.DeviceAccessService;
import com.example.smarthome.server.telegram.Bot;
import com.example.smarthome.server.telegram.MessageExecutor;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.Message;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;

import java.util.ArrayList;
import java.util.List;

public class HomeControlLevel implements AnswerCreator {

    private static final HomeControlLevel instance = new HomeControlLevel();

    private static final DeviceAccessService service = DeviceAccessService.getInstance();
    private static final Bot bot = Bot.getInstance();

    // ************************************* MESSAGES ************************************************
    private static final String buttonInvalid = "Кнопка недействительна";
    private static final String tokenSuccessGen = "Ваш токен успешно сгенерирован!\nОн требуется для подключения Вашей " +
            "Raspberry PI к серверу.\nПожалуйста, скопируйте и вставьте Ваш токен в соответствующий раздел в приложении." +
            "\n\n\u21E7\u21E7\u21E7 ВАШ ТОКЕН \u21E7\u21E7\u21E7";
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
                    // goToDevicesLevel(null, msg);
                    break;
                case "users":
                    // goToUsersLevel(msg.getId(), null);
                    break;
                case "token_gen":
                    MessageExecutor.execute(bot, new Message(user.getChatId(), tokenSuccessGen)
                            .setMessageId(msg.getId()));
                    MessageExecutor.execute(bot, new Message(user.getChatId(),
                            service.createToken(user.getChatId())));
                    MenuLevel.getInstance().goToMain(user, msg);
                    break;
                case "back":
                    MenuLevel.getInstance().goToMain(user, msg);
                    break;
                default:
                    MessageExecutor.execute(bot, new AnswerCallback(msg.getCallbackId(), buttonInvalid));
            }
    }

    public void goToHomeControlLevel(UserInstance user, IncomingMessage msg) {
        if (service.isChannelExist(user.getChatId())) {
            MessageExecutor.execute(bot, new InlineKeyboardMessage(user.getChatId(), homeControl, homeControlButtons)
                    .setNumOfColumns(2)
                    .setMessageId(msg.getId())
                    .hasBackButton(true));
            user.setCurrentLvl(instance);
        } else {
            MessageExecutor.execute(bot, new AnswerCallback(msg.getCallbackId(), channelNotFound)
                    .hasAlert(true));

            if (user.getCurrentLvl() != MenuLevel.getInstance()) {
                MenuLevel.getInstance().goToMain(user, msg);
            }
        }
    }
}
