package com.example.smarthome.server.telegram.scenario.levels.home_control.device;

import com.example.smarthome.server.entity.Device;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.exceptions.UserNotFoundException;
import com.example.smarthome.server.service.DeviceAccessService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.example.smarthome.server.connection.ClientAPI.getChannel;
import static com.example.smarthome.server.connection.ClientAPI.getDevices;
import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.HomeControlLevel.goToHomeControlLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.DeviceLevel.goToDeviceLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.DeviceCreationLevel.goToDeviceCreationLevel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DevicesLevel implements AnswerCreator {

    @Getter
    private static final DevicesLevel instance = new DevicesLevel();

    private static final Logger log = LoggerFactory.getLogger(DevicesLevel.class);
    private static final DeviceAccessService service = DeviceAccessService.getInstance();

    private static final Pattern PATTERN = Pattern.compile("[_]");

    // ************************************* MESSAGES *************************************************
    private static final String buttonInvalid = "Кнопка недействительна";
    private static final String devicesNotFound = "Устройства не обнаружены. Вы можете добавить их прямо сейчас";
    private static final String devicesMsg = "Нажмите на существующее устройство или добавьте новое";

    @Override
    public boolean create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {
            String[] arr = PATTERN.split(msg.getText());
            String cmd = arr[0];

            switch (cmd) {
                case "add":
                    goToDeviceCreationLevel(user, msg, () -> EmojiCallback.next(msg.getCallbackId()));
                    break;
                case "back":
                    goToHomeControlLevel(user, msg, () -> EmojiCallback.back(msg.getCallbackId()));
                    break;
                case "device":
                    int deviceId = Integer.parseInt(arr[1]);
                    goToDeviceLevel(user, msg, deviceId, () -> {
                        user.setCurrentLvl(DeviceLevel.getInstance());
                        EmojiCallback.next(msg.getCallbackId());
                    });
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

    public static void goToDevicesLevel(UserInstance user, IncomingMessage msg, CallbackAction action) {
        try {
            List<CallbackButton> devices = new ArrayList<>();

            for (Device device : getDevices(getChannel(user.getChatId()))) {
                devices.add(new CallbackButton(device.getName(), "device_" + device.getId()));
            }

            InlineKeyboardMessage answer = new InlineKeyboardMessage(user.getChatId(),
                    devices.isEmpty() ? devicesNotFound : devicesMsg, devices)
                    .hasBackButton(true)
                    .setMessageId(msg.getId())
                    .setNumOfColumns(2);

            if (UserRole.valueOf(service.getUser(user.getChatId()).getRole().toUpperCase()).getCode() > 0) {
                answer.hasAddButton(true);
            }

            executeAsync(answer, () -> {
                user.setCurrentLvl(instance);
                if (action != null) action.process();
            });
        } catch (ChannelNotFoundException | UserNotFoundException e) {
            log.warn(e.getMessage());
            goToHomeControlLevel(user, msg, null);
        }
    }
}
