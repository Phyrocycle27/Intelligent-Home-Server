package com.example.smarthome.server.telegram.scenario.levels.administration_users;

import com.example.smarthome.server.service.DeviceAccessService;
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

import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UserLevel.goToUserLevel;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UsersLevel.goToUsersLevel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserConfirmRemoveLevel implements AnswerCreator {

    @Getter
    private static final UserConfirmRemoveLevel instance = new UserConfirmRemoveLevel();

    private static final DeviceAccessService service = DeviceAccessService.getInstance();

    private static final Pattern PATTERN = Pattern.compile("[_]");

    // ************************************* MESSAGES *************************************************
    private static final String removeConfirmationUser = "Вы действительно хотите удалить этого пользователя?";
    private static final String userRemoved = "Пользователь удалён";
    private static final String buttonInvalid = "Кнопка недействительна";

    @Override
    public boolean create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {
            String[] arr = PATTERN.split(msg.getText());

            String cmd = arr[0];
            long userId = arr.length > 1? Long.parseLong(arr[1]): 0;

            switch (cmd) {
                case "confirmRemove":
                    service.deleteUser(userId);
                    goToUsersLevel(user, msg);
                    EmojiCallback.success(msg.getCallbackId());
                    break;
                case "cancel":
                    goToUserLevel(user, msg, userId);
                    EmojiCallback.back(msg.getCallbackId());
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

    public static void goToUserConfirmRemoveLevel(UserInstance user, IncomingMessage msg, long userId) {
        executeAsync(new InlineKeyboardMessage(user.getChatId(), removeConfirmationUser,
                new ArrayList<CallbackButton>() {{
                    add(new CallbackButton("Подтвердить", "confirmRemove_" + userId));
                    add(new CallbackButton("Отмена", "cancel_" + userId));
                }})
                .setMessageId(msg.getId())
                .setNumOfColumns(2), () -> user.setCurrentLvl(instance));
    }
}
