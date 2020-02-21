package com.example.smarthome.server.telegram.scenario.levels.administration_users;

import com.example.smarthome.server.entity.TelegramUser;
import com.example.smarthome.server.service.DeviceAccessService;
import com.example.smarthome.server.telegram.Bot;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import static com.example.smarthome.server.telegram.MessageExecutor.execute;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UserConfirmRemoveLevel.goToUserConfirmRemoveLevel;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UsersLevel.goToUsersLevel;

public class UserLevel implements AnswerCreator {

    private static final UserLevel instance = new UserLevel();

    private static final DeviceAccessService service = DeviceAccessService.getInstance();
    private static final Bot bot = Bot.getInstance();

    private UserLevel() {
    }

    public static UserLevel getInstance() {
        return instance;
    }

    @Override
    public void create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {

            String[] arr = msg.getText().split("[_]");
            String cmd = arr[0];

            switch (cmd) {
                case "back":
                    goToUsersLevel(user, msg);
                    break;
                case "remove":
                    long userId = Long.parseLong(arr[1]);
                    goToUserConfirmRemoveLevel(user, msg, userId);
                    break;
            }
        }
    }

    public static void goToUserLevel(UserInstance userInstance, IncomingMessage msg, long userId) {
        TelegramUser user = service.getUser(userId);
        execute(bot, new InlineKeyboardMessage(userInstance.getChatId(),
                String.format("<i>%s</i>\nУровень доступа: %s\nДата добавления: %s",
                        bot.getUserName(userId), user.getRole(), user.getAdditionDate()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
                new ArrayList<CallbackButton>() {{
                    add(new CallbackButton("Удалить", "remove_" + userId));
                }})
                .setMessageId(msg.getId())
                .hasBackButton(true));

        userInstance.setCurrentLvl(instance);
    }
}
