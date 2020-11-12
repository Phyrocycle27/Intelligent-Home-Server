package com.example.smarthome.server.telegram.scenario.levels.administration_users;

import com.example.smarthome.server.entity.TelegramUser;
import com.example.smarthome.server.exceptions.UserNotFoundException;
import com.example.smarthome.server.service.DeviceAccessService;
import com.example.smarthome.server.telegram.Bot;
import com.example.smarthome.server.telegram.CallbackAction;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.UserRole;
import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UserConfirmRemoveLevel.goToUserConfirmRemoveLevel;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UserSetupRoleLevel.goToUserSetupRoleLevel;
import static com.example.smarthome.server.telegram.scenario.levels.administration_users.UsersLevel.goToUsersLevel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserLevel implements AnswerCreator {

    @Getter
    private static final UserLevel instance = new UserLevel();

    private static final DeviceAccessService service = DeviceAccessService.getInstance();
    private static final Logger log = LoggerFactory.getLogger(UserLevel.class);
    private static final Bot bot = Bot.getInstance();

    private static final Pattern PATTERN = Pattern.compile("[_]");

    // ************************************* MESSAGES *************************************************
    private static final String userNotFound = "Пользователь не найден";
    private static final String buttonInvalid = "Кнопка недействительна";

    @Override
    public boolean create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {

            String[] arr = PATTERN.split(msg.getText());
            String cmd = arr[0];
            long userId = arr.length > 1 ? Long.parseLong(arr[1]) : 0;

            switch (cmd) {
                case "back":
                    goToUsersLevel(user, msg, () -> EmojiCallback.back(msg.getCallbackId()));
                    break;
                case "remove":
                    goToUserConfirmRemoveLevel(user, msg, userId, () -> EmojiCallback.next(msg.getCallbackId()));
                    break;
                case "change-role":
                    goToUserSetupRoleLevel(user, msg, userId, this, () -> EmojiCallback.next(msg.getCallbackId()));
                    break;
                default:
                    executeAsync(new AnswerCallback(msg.getCallbackId(), buttonInvalid),
                            () -> user.setProcessing(false));
            }
            // если сообщение успешно обработано, то возвращаем истину
            return true;
        }
        // иначе, если содержание сообщения не может быть обработано уровнем, возвращаем ложь
        return false;
    }

    public static void goToUserLevel(UserInstance userInstance, IncomingMessage msg, long userId, CallbackAction action) {
        try {
            TelegramUser user = service.getUser(userId);
            TelegramUser currUser = service.getUser(userInstance.getChatId());

            executeAsync(new InlineKeyboardMessage(userInstance.getChatId(),
                    String.format("<i>%s</i>\nУровень доступа: %s\nДата добавления: %s",
                            bot.getUserName(userId), user.getRole(), user.getAdditionDate()
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
                    new ArrayList<>() {{
                        int code = UserRole.valueOf(currUser.getRole().toUpperCase()).getCode();

                        if (code >= UserRole.CREATOR.getCode() || userInstance.getChatId() == userId)
                            add(new CallbackButton("Удалить", "remove_" + userId));
                        if (code >= UserRole.CREATOR.getCode() && userInstance.getChatId() != userId) {
                            add(new CallbackButton("Изменить роль", "change-role_" + userId));
                        }
                    }})
                    .setMessageId(msg.getId())
                    .hasBackButton(true)
                    .setNumOfColumns(2), () -> {
                userInstance.setCurrentLvl(instance);
                if (action != null) action.process();
            });
        } catch (UserNotFoundException e) {
            log.error(e.getMessage());
            executeAsync(new AnswerCallback(msg.getCallbackId(), "Пользователь не найден")
                    .hasAlert(true));
        }
    }
}
