package com.example.smarthome.server.telegram.scenario.levels.administration_users;

import com.example.smarthome.server.exceptions.UserAlreadyExistsException;
import com.example.smarthome.server.service.DeviceAccessService;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.UserRole;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UserAdditionLevel.goToUserAdditionLevel;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UsersLevel.goToUsersLevel;

public class UserSetupRoleLevel implements AnswerCreator {

    private static final UserSetupRoleLevel instance = new UserSetupRoleLevel();

    private static final DeviceAccessService service = DeviceAccessService.getInstance();

    private static final Pattern PATTERN = Pattern.compile("[_]");

    // ************************************* MESSAGES *************************************************
    private static final String chooseRole = "Выберите уровень доступа, который хотите назначить пользователю";

    private UserSetupRoleLevel() {
    }

    public static UserSetupRoleLevel getInstance() {
        return instance;
    }

    @Override
    public void create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {

            String[] arr = PATTERN.split(msg.getText());
            String s = arr[0];
            long userId = arr.length > 1 ? Long.parseLong(arr[1]) : 0;

            switch (s) {
                case "user":
                case "admin":
                    try {
                        service.addUser(user.getChatId(), userId, UserRole.valueOf(s.toUpperCase()));
                        goToUsersLevel(user, msg);
                        EmojiCallback.success(msg.getCallbackId());
                    } catch (UserAlreadyExistsException e) {
                        e.printStackTrace();
                    }
                    break;
                case "back":
                    goToUserAdditionLevel(user, msg);
                    EmojiCallback.back(msg.getCallbackId());
            }
        }
    }

    public static void goToUserSetupRoleLevel(UserInstance user, IncomingMessage msg, long userId) {
        executeAsync(new InlineKeyboardMessage(user.getChatId(), chooseRole,
                new ArrayList<CallbackButton>() {{
                    add(new CallbackButton("Admin", "admin_" + userId));
                    add(new CallbackButton("User", "user_" + userId));
                }})
                .setMessageId(msg.getId())
                .setNumOfColumns(2)
                .hasBackButton(true), () -> user.setCurrentLvl(instance));
    }
}
