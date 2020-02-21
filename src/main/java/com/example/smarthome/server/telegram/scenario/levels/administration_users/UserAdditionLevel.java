package com.example.smarthome.server.telegram.scenario.levels.administration_users;

import com.example.smarthome.server.exceptions.UserAlreadyExistsException;
import com.example.smarthome.server.service.DeviceAccessService;
import com.example.smarthome.server.telegram.Bot;
import com.example.smarthome.server.telegram.MessageExecutor;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;

public class UserAdditionLevel implements AnswerCreator {

    private static final UserAdditionLevel instance = new UserAdditionLevel();

    private static final DeviceAccessService service = DeviceAccessService.getInstance();
    private static final Bot bot = Bot.getInstance();

    // ************************************* MESSAGES *************************************************
    private static final String sendUserContact = "Отправьте контакт пользователя, которого хотите добавить";

    private UserAdditionLevel() {
    }

    public static UserAdditionLevel getInstance() {
        return instance;
    }


    @Override
    public void create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK && msg.getText().equals("back")) {
            UsersLevel.goToUsersLevel(user, msg);
        } else if (msg.getType() == MessageType.CONTACT) {
            try {
                service.addUser(user.getChatId(), Long.parseLong(msg.getText()), "user");
                UsersLevel.goToUsersLevel(user, msg);
                // пользователь успешно добавлен (AnswerCallback)
            } catch (UserAlreadyExistsException e) {
                UsersLevel.goToUsersLevel(user, msg);
            } finally {
                MessageExecutor.delete(bot, user.getChatId(), user.getLastMessageId());
                user.setLastMessageId(0);
            }
        }
    }

    public static void goToUserAdditionLevel(UserInstance user, IncomingMessage msg) {
        MessageExecutor.execute(bot, new InlineKeyboardMessage(user.getChatId(), sendUserContact, null)
                .hasBackButton(true).setMessageId(msg.getId()));
        user.setCurrentLvl(UserAdditionLevel.getInstance());
        user.setLastMessageId(msg.getId());

        user.setCurrentLvl(instance);
    }
}
