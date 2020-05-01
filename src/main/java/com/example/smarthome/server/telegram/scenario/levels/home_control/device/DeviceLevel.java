package com.example.smarthome.server.telegram.scenario.levels.home_control.device;

import com.example.smarthome.server.entity.Device;
import com.example.smarthome.server.entity.signal.SignalType;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.exceptions.OutputNotFoundException;
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

import static com.example.smarthome.server.connection.ClientAPI.*;
import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.HomeControlLevel.goToHomeControlLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.DeviceConfirmRemoveLevel.goToDeviceConfirmRemoveLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.DevicePwmControlLevel.goToDevicePwmControlLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.DevicesLevel.goToDevicesLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.DeviceEditingLevel.goToDeviceEditingLevel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceLevel implements AnswerCreator {

    @Getter
    private static final DeviceLevel instance = new DeviceLevel();

    private static final Logger log = LoggerFactory.getLogger(DeviceLevel.class);
    private static final DeviceAccessService service = DeviceAccessService.getInstance();

    private static final Pattern PATTERN = Pattern.compile("[_]");

    // ************************************* MESSAGES *************************************************
    private static final String buttonInvalid = "Кнопка недействительна";
    private static final String deviceNotFound = "Устройство не найдено";
    private static final String deviceOff = "Устройство выключено";
    private static final String deviceOn = "Устройство включено";

    @Override
    public boolean create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {

            String[] arr = PATTERN.split(msg.getText());
            String cmd = arr[0];
            int deviceId = arr.length > 1 ? Integer.parseInt(arr[1]) : 0;

            try {
                switch (cmd) {
                    case "back":
                        goToDevicesLevel(user, msg, () -> EmojiCallback.back(msg.getCallbackId()));
                        break;
                    case "off":
                    case "on":
                        setDigitalState(getChannel(user.getChatId()), deviceId, cmd.equals("on"));
                        goToDeviceLevel(user, msg, deviceId, () -> EmojiCallback.success(msg.getCallbackId()));
                        break;
                    case "remove":
                        goToDeviceConfirmRemoveLevel(user, msg, deviceId, () -> EmojiCallback.next(msg.getCallbackId()));
                        break;
                    case "edit":
                        goToDeviceEditingLevel(user, msg, deviceId, () -> EmojiCallback.next(msg.getCallbackId()));
                        break;
                    case "control":
                        goToDevicePwmControlLevel(user, msg, deviceId, () -> EmojiCallback.next(msg.getCallbackId()));
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
            // если сообщение успешно обработано, то возвращаем истину
            return true;
        }
        // иначе, если содержание сообщения не может быть обработано уровнем, возвращаем ложь
        return false;
    }

    public static void goToDeviceLevel(UserInstance user, IncomingMessage msg, int deviceId, CallbackAction action) {
        try {
            Device device = getDevice(getChannel(user.getChatId()), deviceId);
            List<CallbackButton> buttons = new ArrayList<>();
            String inversion = device.getReverse() ? "включена" : "выключена";
            String currStateText = "";
            String signalType = "";

            if (device.getGpio().getType() == SignalType.DIGITAL) {
                boolean currState = getDigitalState(getChannel(user.getChatId()), device.getId());
                signalType = "цифовой";

                currStateText = currState ? "включено" : "выключено";

                if (currState) {
                    buttons.add(new CallbackButton("Выключить", "off_" + device.getId()));
                } else {
                    buttons.add(new CallbackButton("Включить", "on_" + device.getId()));
                }
            } else if (device.getGpio().getType() == SignalType.PWM) {
                int currSignal = getPwmSignal(getChannel(user.getChatId()), device.getId());
                signalType = "ШИМ";

                if (currSignal >= 768) {
                    currStateText = "очень сильный";
                } else if (currSignal >= 512) {
                    currStateText = "сильный";
                } else if (currSignal >= 256) {
                    currStateText = "средний";
                } else if (currSignal >= 1) {
                    currStateText = "слабый";
                } else {
                    currStateText = "выключено";
                }

                buttons.add(new CallbackButton("Управлять", "control_" + device.getId()));
            }

            if (UserRole.valueOf(service.getUser(user.getChatId()).getRole().toUpperCase()).getCode() > 0) {
                buttons.add(new CallbackButton("Удалить", "remove_" + device.getId()));
                buttons.add(new CallbackButton("Редактировать", "edit_" + device.getId()));
            }

            executeAsync(new InlineKeyboardMessage(user.getChatId(), String.format("<b>%s</b>\n" +
                            "Текущее состояние: <i>%s</i>\n" +
                            "Тип сигнала: <i>%s</i>\n" +
                            "Инверсия: <i>%s</i>\n" +
                            "GPIO-пин: <i>%d</i>",
                    device.getName(), currStateText, signalType, inversion,
                    device.getGpio().getGpio()), buttons)
                    .setMessageId(msg.getId())
                    .hasBackButton(true), () -> {
                user.setCurrentLvl(instance);
                if (action != null) action.process();
            });
        } catch (OutputNotFoundException e) {
            log.warn(e.getMessage());
            goToDevicesLevel(user, msg, () -> executeAsync(new AnswerCallback(msg.getCallbackId(), deviceNotFound)));
        } catch (ChannelNotFoundException | UserNotFoundException e) {
            log.warn(e.getMessage());
            goToHomeControlLevel(user, msg, null);
        }
    }
}
