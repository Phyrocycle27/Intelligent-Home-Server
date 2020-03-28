package com.example.smarthome.server.telegram.scenario.levels.home_control.device;

import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.telegram.CallbackAction;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.example.smarthome.server.connection.ClientAPI.deleteOutput;
import static com.example.smarthome.server.connection.ClientAPI.getChannel;
import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.HomeControlLevel.goToHomeControlLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.DeviceLevel.goToDeviceLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.DevicesLevel.goToDevicesLevel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceConfirmRemoveLevel implements AnswerCreator {

    @Getter
    private static final DeviceConfirmRemoveLevel instance = new DeviceConfirmRemoveLevel();

    private static final Logger log = LoggerFactory.getLogger(DeviceConfirmRemoveLevel.class);

    private static final Pattern PATTERN = Pattern.compile("[_]");

    // ************************************* MESSAGES *************************************************
    private static final String removeConfirmationDevice = "Вы действительно хотите удалить это устройство?";
    private static final String buttonInvalid = "Кнопка недействительна";

    @Override
    public boolean create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {

            String[] arr = PATTERN.split(msg.getText());
            String cmd = arr[0];
            int deviceId = arr.length > 1 ? Integer.parseInt(arr[1]) : 0;

            try {
                switch (cmd) {
                    case "confirmRemove":
                        deleteOutput(getChannel(user.getChatId()), deviceId);
                        goToDevicesLevel(user, msg, () -> EmojiCallback.success(msg.getCallbackId()));
                        break;
                    case "cancel":
                        goToDeviceLevel(user, msg, deviceId, () -> EmojiCallback.back(msg.getCallbackId()));
                        break;
                    default:
                        executeAsync(new AnswerCallback(msg.getCallbackId(), buttonInvalid),
                                () -> user.setProcessing(false));
                }
            } catch (ChannelNotFoundException e) {
                log.warn(e.getMessage());
                goToHomeControlLevel(user, msg, null);
            }
            // если сообщение успешно обработано, то возвращаем истину
            return true;
        }
        // иначе, если содержание сообщения не может быть обработано уровнем, возвращаем ложь
        return false;
    }

    public static void goToDeviceConfirmRemoveLevel(UserInstance user, IncomingMessage msg,
                                                    int deviceId, CallbackAction action) {

        executeAsync(new InlineKeyboardMessage(user.getChatId(), removeConfirmationDevice,
                new ArrayList<CallbackButton>() {{
                    add(new CallbackButton("Подтвердить", "confirmRemove_" + deviceId));
                    add(new CallbackButton("Отмена", "cancel_" + deviceId));
                }})
                .setMessageId(msg.getId())
                .setNumOfColumns(2), () -> {
            user.setCurrentLvl(instance);
            if (action != null) action.process();
        });
    }
}
