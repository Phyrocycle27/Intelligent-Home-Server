package com.example.smarthome.server.telegram.scenario.levels.administration_users;

import com.example.smarthome.server.service.DeviceAccessService;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UserLevel.goToUserLevel;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UsersLevel.goToUsersLevel;

public class UserConfirmRemoveLevel implements AnswerCreator {

    private static final UserConfirmRemoveLevel instance = new UserConfirmRemoveLevel();

    private static final DeviceAccessService service = DeviceAccessService.getInstance();

    private static final Pattern PATTERN = Pattern.compile("[_]");

    // ************************************* MESSAGES *************************************************
    private static final String removeConfirmationUser = "Вы действительно хотите удалить этого пользователя?";
    private static final String userRemoved = "Пользователь удалён";

    private UserConfirmRemoveLevel() {
    }

    public static UserConfirmRemoveLevel getInstance() {
        return instance;
    }

    @Override
    public void create(UserInstance user, IncomingMessage msg) {
        String[] arr = PATTERN.split(msg.getText());

        String cmd = arr[0];
        long userId = Long.parseLong(arr[1]);

        if (cmd.equals("confirmRemove")) {
            service.deleteUser(userId);
            goToUsersLevel(user, msg);
            EmojiCallback.success(msg.getCallbackId());
        } else if (cmd.equals("cancel")) {
            goToUserLevel(user, msg, userId);
            EmojiCallback.back(msg.getCallbackId());
        }
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
