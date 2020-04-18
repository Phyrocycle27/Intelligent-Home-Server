package com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels;

import com.example.smarthome.server.entity.Output;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.exceptions.OutputNotFoundException;
import com.example.smarthome.server.telegram.CallbackAction;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;
import com.example.smarthome.server.telegram.scenario.MessageProcessor;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.example.smarthome.server.connection.ClientAPI.*;
import static com.example.smarthome.server.telegram.MessageExecutor.deleteAsync;
import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.HomeControlLevel.goToHomeControlLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.DevicesLevel.goToDevicesLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.DeviceEditingLevel.goToChoice;

public class DeviceEditor {

    private static final Logger log = LoggerFactory.getLogger(DeviceEditor.class);

    private static final String deviceNotFound = "Устройство не найдено";

    @Setter(value = AccessLevel.PACKAGE)
    @Getter(value = AccessLevel.PACKAGE)
    private MessageProcessor currEditingLvl;
    private final UserInstance user;
    @Getter(value = AccessLevel.PACKAGE)
    private final Output editingOutput;

    DeviceEditor(UserInstance user, int deviceId) throws ChannelNotFoundException, OutputNotFoundException {
        editingOutput = getOutput(getChannel(user.getChatId()), deviceId);
        this.user = user;
    }

    boolean process(IncomingMessage msg) {
        if (currEditingLvl.getClass().equals(SetupNameLevel.class)) {
            String deviceName = (String) currEditingLvl.process(user, msg);

            if (deviceName != null && !deviceName.isEmpty()) {
                editingOutput.setName(deviceName);
                deleteAsync(user.getChatId(), user.getLastMessageId(), () -> user.setLastMessageId(0));
                update(msg, null);
                return true;
            }
        } else if (currEditingLvl.getClass().equals(SetupSignalInversionLevel.class)) {
            Boolean inversion = (Boolean) currEditingLvl.process(user, msg);

            if (inversion != null) {
                editingOutput.setReverse(inversion);
                update(msg, () -> EmojiCallback.success(msg.getCallbackId()));
                return true;
            }
        }
        return false;
    }

    void destroy() {
        user.setDeviceEditor(null);
    }

    private void update(IncomingMessage msg, CallbackAction action) {
        try {
            updateOutput(getChannel(user.getChatId()), editingOutput);
            goToChoice(user, msg, () -> {
                currEditingLvl = null;
                if (action != null) action.process();
            });
        } catch (OutputNotFoundException e) {
            log.warn(e.getMessage());
            goToDevicesLevel(user, msg, () -> executeAsync(new AnswerCallback(msg.getCallbackId(), deviceNotFound)));
            destroy();
        } catch (ChannelNotFoundException e) {
            log.warn(e.getMessage());
            goToHomeControlLevel(user, msg, null);
            destroy();
        }
    }
}
