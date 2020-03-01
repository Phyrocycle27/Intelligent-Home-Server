package com.example.smarthome.server.telegram.scenario.levels.administration_users;

import com.example.smarthome.server.entity.TelegramUser;
import com.example.smarthome.server.exceptions.UserNotFoundException;
import com.example.smarthome.server.service.DeviceAccessService;
import com.example.smarthome.server.telegram.Bot;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.UserRole;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;

import java.util.ArrayList;
import java.util.List;

import static com.example.smarthome.server.telegram.MessageExecutor.execute;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UserAdditionLevel.goToUserAdditionLevel;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UserLevel.goToUserLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.HomeControlLevel.goToHomeControlLevel;

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
                    goToUserAdditionLevel(user, msg);
                    EmojiCallback.next(msg.getCallbackId());
                    break;
                case "back":
                    goToHomeControlLevel(user, msg);
                    EmojiCallback.back(msg.getCallbackId());
                    break;
                default:
                    goToUserLevel(user, msg, Long.parseLong(msg.getText()));
                    EmojiCallback.next(msg.getCallbackId());
            }
    }

    public static void goToUsersLevel(UserInstance userInstance, IncomingMessage msg) {
        List<CallbackButton> users = new ArrayList<>();

        for (TelegramUser user : service.getUsers(userInstance.getChatId())) {
            users.add(new CallbackButton(bot.getUserName(user.getUserId()), String.valueOf(user.getUserId())));
        }

        InlineKeyboardMessage answer = new InlineKeyboardMessage(userInstance.getChatId(), allowedUsersMessage, users)
                .hasBackButton(true)
                .setMessageId(msg.getId());

        try {
            if (service.getUser(userInstance.getChatId()).getRole().equals(UserRole.CREATOR.getName())) {
                answer.hasAddButton(true);
            }

            execute(answer);
        } catch (UserNotFoundException e) {
            e.printStackTrace();
        }

        userInstance.setCurrentLvl(instance);
    }
}
