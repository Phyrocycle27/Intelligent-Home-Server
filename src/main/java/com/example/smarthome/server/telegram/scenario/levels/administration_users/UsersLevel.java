package com.example.smarthome.server.telegram.scenario.levels.administration_users;

import com.example.smarthome.server.entity.TelegramUser;
import com.example.smarthome.server.exceptions.UserNotFoundException;
import com.example.smarthome.server.service.DeviceAccessService;
import com.example.smarthome.server.telegram.Bot;
import com.example.smarthome.server.telegram.CallbackAction;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.UserRole;
import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UserAdditionLevel.goToUserAdditionLevel;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UserLevel.goToUserLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.HomeControlLevel.goToHomeControlLevel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UsersLevel implements AnswerCreator {

    @Getter
    private static final UsersLevel instance = new UsersLevel();

    private static final DeviceAccessService service = DeviceAccessService.getInstance();
    private static final Bot bot = Bot.getInstance();

    private static final Pattern p = Pattern.compile("[_]");

    // ************************************* MESSAGES ************************************************
    private static final String allowedUsersMessage = "Список пользователей, имеющих доступ к вашему дому";
    private static final String buttonInvalid = "Кнопка недействительна";

    @Override
    public boolean create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {
            String[] arr = p.split(msg.getText());
            String cmd = arr[0];

            switch (msg.getText()) {
                case "add":
                    goToUserAdditionLevel(user, msg, () -> EmojiCallback.next(msg.getCallbackId()));
                    break;
                case "back":
                    goToHomeControlLevel(user, msg, () -> EmojiCallback.back(msg.getCallbackId()));
                    break;
                case "id":
                    long userId = Long.parseLong(arr[1]);
                    goToUserLevel(user, msg, userId, () -> EmojiCallback.next(msg.getCallbackId()));
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

    public static void goToUsersLevel(UserInstance userInstance, IncomingMessage msg, CallbackAction action) {
        List<CallbackButton> users = new ArrayList<>();

        for (TelegramUser user : service.getUsers(userInstance.getChatId())) {
            users.add(new CallbackButton(bot.getUserName(user.getUserId()), "id_" + user.getUserId()));
        }

        InlineKeyboardMessage answer = new InlineKeyboardMessage(userInstance.getChatId(), allowedUsersMessage, users)
                .hasBackButton(true)
                .setMessageId(msg.getId());

        try {
            if (service.getUser(userInstance.getChatId()).getRole().equals(UserRole.CREATOR.getName())) {
                answer.hasAddButton(true);
            }

            executeAsync(answer, () -> {
                userInstance.setCurrentLvl(instance);
                if (action != null) action.process();
            });
        } catch (UserNotFoundException e) {
            e.printStackTrace();
            goToHomeControlLevel(userInstance, msg, null);
        }
    }
}
