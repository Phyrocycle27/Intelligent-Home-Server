package com.example.smarthome.server.telegram.scenario.levels.administration_users;

import com.example.smarthome.server.entity.TelegramUser;
import com.example.smarthome.server.service.DeviceAccessService;
import com.example.smarthome.server.telegram.Bot;
import com.example.smarthome.server.telegram.MessageExecutor;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;
import com.example.smarthome.server.telegram.scenario.levels.HomeControlLevel;

import java.util.ArrayList;
import java.util.List;

public class UsersLevel implements AnswerCreator {

    private static final UsersLevel instance = new UsersLevel();

    private static final DeviceAccessService service = DeviceAccessService.getInstance();
    private static final Bot bot = Bot.getInstance();

    // ************************************* MESSAGES ************************************************
    private static final String allowedUsersMessage = "Список пользователей, имеющих доступ к вашему дому";

    private UsersLevel() {
    }

    public static UsersLevel getInstance() {
        return instance;
    }

    @Override
    public void create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK)
            switch (msg.getText()) {
                case "add":
                    UserAdditionLevel.goToUserAdditionLevel(user, msg);
                    break;
                case "back":
                    HomeControlLevel.goToHomeControlLevel(user, msg);
                    break;
                default:
                    UserLevel.goToUserLevel(user, msg, Long.parseLong(msg.getText()));
            }
    }

    public static void goToUsersLevel(UserInstance userInstance, IncomingMessage msg) {
        List<CallbackButton> users = new ArrayList<>();

        for (TelegramUser user : service.getUsers(userInstance.getChatId())) {
            users.add(new CallbackButton(bot.getUserName(user.getUserId()), String.valueOf(user.getUserId())));
        }

        MessageExecutor.execute(bot, new InlineKeyboardMessage(userInstance.getChatId(), allowedUsersMessage, users)
                .hasAddButton(true)
                .hasBackButton(true)
                .setMessageId(msg.getId()));

        userInstance.setCurrentLvl(UsersLevel.getInstance());
    }
}
