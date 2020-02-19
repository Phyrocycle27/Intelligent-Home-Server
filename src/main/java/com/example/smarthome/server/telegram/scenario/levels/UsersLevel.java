package com.example.smarthome.server.telegram.scenario.levels;

import com.example.smarthome.server.telegram.Bot;
import com.example.smarthome.server.telegram.MessageExecutor;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;

public class UsersLevel implements AnswerCreator {

    private static final UsersLevel instance = new UsersLevel();

    private static final Bot bot = Bot.getInstance();

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
                    MessageExecutor.execute(bot, new InlineKeyboardMessage(user.getChatId(), "Отправьте контакт " +
                            "пользователя, которого хотите добавить", null)
                            .hasBackButton(true).setMessageId(msg.getId()));
                    // currentLvl = userAdditionLvl;
                    // lastMessageId = msg.getId();
                    break;
                case "back":
                    HomeControlLevel.getInstance().goToHomeControlLevel(user, msg);
                    break;
                default:
                    // goToUserLevel(msg.getId(), Long.parseLong(msg.getText()));
            }
    }
}
