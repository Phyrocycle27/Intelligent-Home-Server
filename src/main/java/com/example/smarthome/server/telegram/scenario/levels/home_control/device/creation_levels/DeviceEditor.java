package com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels;

import com.example.smarthome.server.connection.ClientAPI;
import com.example.smarthome.server.entity.Output;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.scenario.MessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.example.smarthome.server.connection.ClientAPI.getChannel;
import static com.example.smarthome.server.connection.ClientAPI.getOutput;
import static com.example.smarthome.server.telegram.MessageExecutor.delete;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.HomeControlLevel.goToHomeControlLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.DeviceEditingLevel.goToChoice;

public class DeviceEditor {

    private static final Logger log = LoggerFactory.getLogger(DeviceEditor.class);

    public MessageProcessor getCurrEditingLvl() {
        return currEditingLvl;
    }

    public void setCurrEditingLvl(MessageProcessor currEditingLvl) {
        this.currEditingLvl = currEditingLvl;
    }

    private MessageProcessor currEditingLvl;
    private UserInstance user;

    private Output editingOutput;

    public Output getEditingOutput() {
        return editingOutput;
    }

    DeviceEditor(UserInstance user, int deviceId) throws ChannelNotFoundException {
        editingOutput = getOutput(getChannel(user.getChatId()), deviceId);
        this.user = user;
    }

    void process(IncomingMessage msg) {
        if (currEditingLvl.getClass().equals(SetupNameLevel.class)) {
            String deviceName = (String) currEditingLvl.process(user, msg);

            if (deviceName != null && !deviceName.isEmpty()) {
                editingOutput.setName(deviceName);
                update(msg);

                delete(user.getChatId(), user.getLastMessageId());
                user.setLastMessageId(0);

            }
        } else if (currEditingLvl.getClass().equals(SetupSignalInversionLevel.class)) {
            Boolean inversion = (Boolean) currEditingLvl.process(user, msg);

            if (inversion != null) {
                editingOutput.setReverse(inversion);
                update(msg);

                EmojiCallback.success(msg.getCallbackId());
            }
        }
    }

    void destroy() {
        user.setDeviceEditor(null);
    }

    private void update(IncomingMessage msg) {
        try {
            ClientAPI.updateOutput(getChannel(user.getChatId()), editingOutput);
            currEditingLvl = null;
            goToChoice(user, msg);
        } catch (ChannelNotFoundException e) {
            log.warn(e.getMessage());
            goToHomeControlLevel(user, msg);
            destroy();
        }
    }
}
