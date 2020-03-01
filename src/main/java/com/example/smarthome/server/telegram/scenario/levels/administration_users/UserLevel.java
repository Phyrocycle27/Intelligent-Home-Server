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
import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.example.smarthome.server.telegram.MessageExecutor.execute;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UserConfirmRemoveLevel.goToUserConfirmRemoveLevel;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UsersLevel.goToUsersLevel;

public class UserLevel implements AnswerCreator {

    private static final UserLevel instance = new UserLevel();

    private static final DeviceAccessService service = DeviceAccessService.getInstance();
    private static final Logger log = LoggerFactory.getLogger(UserLevel.class);
    private static final Bot bot = Bot.getInstance();

    private static final Pattern PATTERN = Pattern.compile("[_]");

    // ************************************* MESSAGES *************************************************
    private static final String userNotFound = "Пользователь не найден";

    private UserLevel() {
    }

    public static UserLevel getInstance() {
        return instance;
    }

    @Override
    public void create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {

            String[] arr = PATTERN.split(msg.getText());
            String cmd = arr[0];

            switch (cmd) {
                case "back":
                    goToUsersLevel(user, msg);
                    EmojiCallback.back(msg.getCallbackId());
                    break;
                case "remove":
                    long userId = Long.parseLong(arr[1]);
                    goToUserConfirmRemoveLevel(user, msg, userId);
                    EmojiCallback.next(msg.getCallbackId());
                    break;
            }
        }
    }

    public static void goToUserLevel(UserInstance userInstance, IncomingMessage msg, long userId) {
        try {
            TelegramUser user = service.getUser(userId);
            execute(new InlineKeyboardMessage(userInstance.getChatId(),
                    String.format("<i>%s</i>\nУровень доступа: %s\nДата добавления: %s",
                            bot.getUserName(userId), user.getRole(), user.getAdditionDate()
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
                    new ArrayList<CallbackButton>() {{
                        if (service.getUser(userInstance.getChatId()).getRole().equals(UserRole.CREATOR.getName()) ||
                                userInstance.getChatId() == userId)
                            add(new CallbackButton("Удалить", "remove_" + userId));
                    }})
                    .setMessageId(msg.getId())
                    .hasBackButton(true));

            userInstance.setCurrentLvl(instance);
        } catch (UserNotFoundException e) {
            log.error(e.getMessage());
            execute(new AnswerCallback(msg.getCallbackId(), "Пользователь не найден")
                    .hasAlert(true));
        }
    }
}
