package com.example.smarthome.server.telegram.scenario.levels.administration_users;

import com.example.smarthome.server.entity.TelegramUser;
import com.example.smarthome.server.exceptions.UserAlreadyExistsException;
import com.example.smarthome.server.exceptions.UserNotFoundException;
import com.example.smarthome.server.service.DeviceAccessService;
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
import java.util.regex.Pattern;

import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UserAdditionLevel.goToUserAdditionLevel;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UserLevel.goToUserLevel;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UsersLevel.goToUsersLevel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserSetupRoleLevel implements AnswerCreator {

    @Getter
    private static final UserSetupRoleLevel instance = new UserSetupRoleLevel();

    private static final DeviceAccessService service = DeviceAccessService.getInstance();

    private static final Pattern PATTERN = Pattern.compile("[_]");

    // ************************************* MESSAGES *************************************************
    private static final String chooseRole = "Выберите уровень доступа, который хотите назначить пользователю";
    private static final String buttonInvalid = "Кнопка недействительна";

    @Override
    public boolean create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {

            String[] arr = PATTERN.split(msg.getText());
            String s = arr[0];
            long userId = arr.length > 1 ? Long.parseLong(arr[1]) : 0;

            switch (s) {
                case "user":
                case "admin":
                    try {
                        service.addUser(user.getChatId(), userId, UserRole.valueOf(s.toUpperCase()));
                        goToUsersLevel(user, msg, () -> EmojiCallback.success(msg.getCallbackId()));
                    } catch (UserAlreadyExistsException e) {
                        e.printStackTrace();
                    }
                    break;
                case "back-to-addition":
                    goToUserAdditionLevel(user, msg, () -> EmojiCallback.back(msg.getCallbackId()));
                    break;
                case "back-to-user":
                    goToUserLevel(user, msg, userId, () -> EmojiCallback.back(msg.getCallbackId()));
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

    public static void goToUserSetupRoleLevel(UserInstance userInstance, IncomingMessage msg, long userId, Object from,
                                              CallbackAction action) {
        TelegramUser tmp = null;
        try {
            tmp = service.getUser(userId);
        } catch (UserNotFoundException ignored) {
        }

        final TelegramUser user = tmp;

        executeAsync(new InlineKeyboardMessage(userInstance.getChatId(), chooseRole,
                new ArrayList<CallbackButton>() {{
                    if (user == null || !user.getRole().equals("admin")) {
                        add(new CallbackButton("Admin", "admin_" + userId));
                    }
                    if (user == null || !user.getRole().equals("user")) {
                        add(new CallbackButton("User", "user_" + userId));
                    }
                    if (from instanceof UserLevel) {
                        add(new CallbackButton("Назад", "back-to-user_" + userId));
                    } else if (from instanceof UserAdditionLevel) {
                        add(new CallbackButton("Назад", "back-to-addition"));
                    }
                }})
                .setMessageId(msg.getId())
                .setNumOfColumns(2), () -> {
            userInstance.setCurrentLvl(instance);
            if (action != null) action.process();
        });
    }
}
