package com.example.smarthome.server.telegram.scenario.levels.home_control;

import com.example.smarthome.server.service.DeviceAccessService;
import com.example.smarthome.server.telegram.CallbackAction;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.Message;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;
import com.example.smarthome.server.telegram.scenario.levels.MenuLevel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.MenuLevel.goToMenuLevel;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UsersLevel.goToUsersLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.DevicesLevel.goToDevicesLevel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HomeControlLevel implements AnswerCreator {

    @Getter
    private static final HomeControlLevel instance = new HomeControlLevel();

    private static final DeviceAccessService service = DeviceAccessService.getInstance();

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
    private static final String lostConnection = "Потеряно соединение с Raspberry PI";
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

    @Override
    public boolean create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {
            switch (msg.getText()) {
                case "devices":
                    goToDevicesLevel(user, msg, () -> EmojiCallback.next(msg.getCallbackId()));
                    break;
                case "users":
                    goToUsersLevel(user, msg, () -> EmojiCallback.next(msg.getCallbackId()));
                    break;
                case "token_gen":
                    sendToken(user, msg);
                    break;
                case "back":
                    goToMenuLevel(user, msg, () -> EmojiCallback.back(msg.getCallbackId()));
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

    public static void goToHomeControlLevel(UserInstance user, IncomingMessage msg, CallbackAction action) {
        if (service.isUserExists(user.getChatId())) {
            if (service.isChannelExist(user.getChatId())) {
                executeAsync(new InlineKeyboardMessage(user.getChatId(), homeControl, homeControlButtons)
                        .setNumOfColumns(2)
                        .setMessageId(msg.getId())
                        .hasBackButton(true), () -> {
                    user.setCurrentLvl(instance);
                    if (action != null) action.process();
                });
            } else {
                executeAsync(new AnswerCallback(msg.getCallbackId(), channelNotFound)
                        .hasAlert(true), () -> user.setProcessing(false));

                if (user.getCurrentLvl() != MenuLevel.getInstance())
                    goToMenuLevel(user, msg, () -> executeAsync(new AnswerCallback(msg.getCallbackId(), lostConnection)
                                    .hasAlert(true)));
            }
        } else {
            executeAsync(new InlineKeyboardMessage(user.getChatId(), tokenNotFound,
                    tokenGenButton)
                    .setMessageId(msg.getId())
                    .hasBackButton(true), () -> {
                user.setCurrentLvl(instance);
                if (action != null) action.process();
            });
        }
    }

    private void sendToken(UserInstance user, IncomingMessage msg) {
        executeAsync(new Message(user.getChatId(), tokenSuccessGen)
                .setMessageId(msg.getId()), () -> EmojiCallback.success(msg.getCallbackId()));
        executeAsync(new Message(user.getChatId(),
                service.createToken(user.getChatId())), () -> goToMenuLevel(user, msg,
                () -> EmojiCallback.success(msg.getCallbackId())));
    }
}
