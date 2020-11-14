package com.example.smarthome.server.telegram.scenario.levels.home_control.device;

import com.example.smarthome.server.entity.Device;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.exceptions.OutputNotFoundException;
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
import java.util.List;
import java.util.regex.Pattern;

import static com.example.smarthome.server.connection.ClientAPI.*;
import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.HomeControlLevel.goToHomeControlLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.DeviceLevel.goToDeviceLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.DevicesLevel.goToDevicesLevel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DevicePwmControlLevel implements AnswerCreator {

    @Getter
    private static final DevicePwmControlLevel instance = new DevicePwmControlLevel();
    private static final Logger log = LoggerFactory.getLogger(DevicePwmControlLevel.class);

    private static final Pattern PATTERN = Pattern.compile("[_]");

    // ************************************* MESSAGES *************************************************
    private static final String buttonInvalid = "Кнопка недействительна";
    private static final String deviceNotFound = "Устройство не найдено";

    @Override
    public boolean create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {

            String[] arr = PATTERN.split(msg.getText());
            String cmd = arr[0];
            long deviceId = arr.length > 1 ? Long.parseLong(arr[1]) : 0;

            try {
                switch (cmd) {
                    case "back":
                        goToDeviceLevel(user, msg, deviceId, () -> EmojiCallback.back(msg.getCallbackId()));
                        break;
                    case "0":
                    case "256":
                    case "512":
                    case "768":
                    case "1024":
                        setPwmSignal(getChannel(user.getChatId()), deviceId, Integer.parseInt(cmd));
                        goToDevicePwmControlLevel(user, msg, deviceId, () -> EmojiCallback.success(msg.getCallbackId()));
                        break;
                    default:
                        executeAsync(new AnswerCallback(msg.getCallbackId(), buttonInvalid),
                                () -> user.setProcessing(false));
                }
            } catch (OutputNotFoundException e) {
                log.warn(e.getMessage());
                goToDevicesLevel(user, msg, () -> executeAsync(new AnswerCallback(msg.getCallbackId(), deviceNotFound)));
            } catch (ChannelNotFoundException e) {
                log.warn(e.getMessage());
                goToHomeControlLevel(user, msg, null);
            }
            return true;
        }
        return false;
    }

    public static void goToDevicePwmControlLevel(UserInstance user, IncomingMessage msg, long deviceId,
                                                 CallbackAction action) {
        try {
            Device device = getDevice(getChannel(user.getChatId()), deviceId);
            int currSignal = getPwmSignal(getChannel(user.getChatId()), deviceId);
            List<CallbackButton> buttons = new ArrayList<>();
            String currSignalText;

            if (currSignal != 0) {
                buttons.add(new CallbackButton("Выключить", "0_" + deviceId));
            }
            if (currSignal > 256 || currSignal == 0) {
                buttons.add(new CallbackButton("Слабый", "256_" + deviceId));
            }
            if (currSignal > 512 || currSignal < 257) {
                buttons.add(new CallbackButton("Средний", "512_" + deviceId));
            }
            if (currSignal > 768 || currSignal < 513) {
                buttons.add(new CallbackButton("Сильный", "768_" + deviceId));
            }
            if (currSignal < 769) {
                buttons.add(new CallbackButton("Очень сильный", "1024_" + deviceId));
            }
            buttons.add(new CallbackButton("Назад", "back_" + deviceId));

            if (currSignal >= 768) {
                currSignalText = "очень сильный";
            } else if (currSignal >= 512) {
                currSignalText = "сильный";
            } else if (currSignal >= 256) {
                currSignalText = "средний";
            } else if (currSignal >= 1) {
                currSignalText = "слабый";
            } else {
                currSignalText = "выключено";
            }

            executeAsync(new InlineKeyboardMessage(user.getChatId(), String.format("<b>%s</b>\n" +
                    "Текущее состояние: <i>%s</i>\n", device.getName(), currSignalText), buttons)
                    .setMessageId(msg.getId()), () -> {
                user.setCurrentLvl(instance);
                if (action != null) action.process();
            });

        } catch (OutputNotFoundException e) {
            log.warn(e.getMessage());
            goToDevicesLevel(user, msg, () -> executeAsync(new AnswerCallback(msg.getCallbackId(), deviceNotFound)));
        } catch (ChannelNotFoundException e) {
            log.warn(e.getMessage());
            goToHomeControlLevel(user, msg, null);
        }
    }
}
