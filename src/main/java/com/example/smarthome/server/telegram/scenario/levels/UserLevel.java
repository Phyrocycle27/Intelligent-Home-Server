package com.example.smarthome.server.telegram.scenario.levels;

import com.example.smarthome.server.telegram.Bot;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;

public class UserLevel implements AnswerCreator {

    private static final UserLevel instance = new UserLevel();

    private static final Bot bot = Bot.getInstance();

    private UserLevel() {
    }

    public static UserLevel getInstance() {
        return instance;
    }

    @Override
    public void create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {
            if (msg.getText().equals("back")) {
                UsersLevel.goToUsersLevel(user, msg);
                return;
            }

            String[] arr = msg.getText().split("[_]");
            String cmd = arr[0];
            long userId = Long.parseLong(arr[1]);

            if (cmd.equals("remove")) {
                /*MessageExecutor.execute(bot, new InlineKeyboardMessage(user.getChatId(), removeConfirmationUser,
                        new ArrayList<CallbackButton>() {{
                            add(new CallbackButton("Подтвердить", "confirmRemove_user_" + userId));
                            add(new CallbackButton("Отмена", "cancel_user_" + userId));
                        }})
                        .setMessageId(msg.getId())
                        .setNumOfColumns(2));*/
                // currentLvl = confirmRemove;
            }
        }
    }
}
