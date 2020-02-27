package com.example.smarthome.server.telegram.scenario.levels.home_control.device;

import com.example.smarthome.server.entity.Output;
import com.example.smarthome.server.entity.UserRole;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.exceptions.UserNotFoundException;
import com.example.smarthome.server.service.DeviceAccessService;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.example.smarthome.server.connection.ClientAPI.*;
import static com.example.smarthome.server.telegram.MessageExecutor.execute;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.HomeControlLevel.goToHomeControlLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.DeviceConfirmRemoveLevel.goToDeviceConfirmRemoveLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.DevicesLevel.goToDevicesLevel;

public class DeviceLevel implements AnswerCreator {

    private static final DeviceLevel instance = new DeviceLevel();

    private static final Logger log = LoggerFactory.getLogger(DeviceLevel.class);
    private static final DeviceAccessService service = DeviceAccessService.getInstance();

    // ************************************* MESSAGES *************************************************
    private static final String removeConfirmationDevice = "Вы действительно хотите удалить это устройство?";
    private static final String buttonInvalid = "Кнопка недействительна";
    private static final String deviceOff = "Устройство выключено";
    private static final String deviceOn = "Устройство включено";


    private DeviceLevel() {
    }

    public static DeviceLevel getInstance() {
        return instance;
    }

    @Override
    public void create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {

            String[] arr = msg.getText().split("[_]");
            String cmd = arr[0];
            int deviceId;

            try {
                switch (cmd) {
                    case "back":
                        goToDevicesLevel(user, msg);
                        EmojiCallback.back(msg.getCallbackId());
                        break;
                    case "off":
                    case "on":
                        deviceId = Integer.parseInt(arr[1]);
                        setDigitalState(getChannel(user.getChatId()), deviceId, cmd.equals("on"));
                        goToDeviceLevel(user, msg, deviceId);
                        EmojiCallback.success(msg.getCallbackId());
                        break;
                    case "remove":
                        deviceId = Integer.parseInt(arr[1]);
                        goToDeviceConfirmRemoveLevel(user, msg, deviceId);
                        EmojiCallback.next(msg.getCallbackId());
                        break;
                    default:
                        execute(new AnswerCallback(msg.getCallbackId(), buttonInvalid));
                }
            } catch (ChannelNotFoundException e) {
                log.warn(e.getMessage());
                goToHomeControlLevel(user, msg);
            }
        }
    }

    public static void goToDeviceLevel(UserInstance user, IncomingMessage msg, int deviceId) {
        try {
            Output output = getOutput(getChannel(user.getChatId()), deviceId);

            if (output == null) goToDevicesLevel(user, msg);
            else {
                boolean currState = getDigitalState(getChannel(user.getChatId()), output.getOutputId());
                String currStateText = currState ? "включено" : "выключено";
                String inversion = output.getReverse() ? "включена" : "выключена";
                String signalType = "";

                switch (output.getType()) {
                    case "digital":
                        signalType = "цифовой";
                        break;
                    case "pwm":
                        signalType = "ШИМ";
                }

                List<CallbackButton> buttons = new ArrayList<CallbackButton>() {{
                    if (currState) add(new CallbackButton("Выключить", "off_" + output.getOutputId()));
                    else add(new CallbackButton("Включить", "on_" + output.getOutputId()));
                    if (UserRole.valueOf(service.getUser(user.getChatId()).getRole().toUpperCase()).getCode() > 0) {
                        add(new CallbackButton("Удалить", "remove_" + output.getOutputId()));
                    }
                }};

                execute(new InlineKeyboardMessage(user.getChatId(), String.format("<b>%s</b>\n" +
                                "Текущее состояние: <i>%s</i>\n" +
                                "Тип сигнала: <i>%s</i>\n" +
                                "Инверсия: <i>%s</i>\n" +
                                "GPIO-пин: <i>%d</i>",
                        output.getName(), currStateText, signalType, inversion,
                        output.getGpio()), buttons)
                        .setMessageId(msg.getId())
                        .hasBackButton(true));
            }
        } catch (ChannelNotFoundException e) {
            log.warn(e.getMessage());
            goToHomeControlLevel(user, msg);
        } catch (UserNotFoundException e) {
            e.printStackTrace();
        }
    }
}
