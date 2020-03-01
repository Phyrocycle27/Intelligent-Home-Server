package com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels;

import com.example.smarthome.server.entity.Output;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.scenario.MessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.example.smarthome.server.connection.ClientAPI.createOutput;
import static com.example.smarthome.server.connection.ClientAPI.getChannel;
import static com.example.smarthome.server.telegram.MessageExecutor.delete;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.HomeControlLevel.goToHomeControlLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.DevicesLevel.goToDevicesLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.SetupGPIOLevel.goToSetupGPIOLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.SetupNameLevel.goToSetupNameLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.SetupSignalInversionLevel.goToSetupSignalInversionLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.SetupSignalTypeLevel.goToSetupSignalTypeLevel;

public class DeviceCreator {

    private static final Logger log = LoggerFactory.getLogger(DeviceCreator.class);

    private MessageProcessor currCreationLvl;
    private UserInstance user;

    private Output creationOutput;

    DeviceCreator(UserInstance user) {
        creationOutput = new Output();
        this.user = user;
    }

    Output getCreationOutput() {
        return creationOutput;
    }

    MessageProcessor getCurrCreationLvl() {
        return currCreationLvl;
    }

    void setCurrCreationLvl(MessageProcessor currCreationLvl) {
        this.currCreationLvl = currCreationLvl;
    }

    void start(IncomingMessage msg) {
        goToSetupNameLevel(user, msg);
        user.getDeviceCreator().setCurrCreationLvl(SetupNameLevel.getInstance());
    }

    void goToPrev(IncomingMessage msg) {
        if (currCreationLvl.getClass().equals(SetupNameLevel.class)) {
            goToDevicesLevel(user, msg);
            destroy();
            EmojiCallback.back(msg.getCallbackId());
        } else if (currCreationLvl.getClass().equals(SetupSignalTypeLevel.class)) {
            goToSetupNameLevel(user, msg);
            EmojiCallback.back(msg.getCallbackId());
        } else if (currCreationLvl.getClass().equals(SetupGPIOLevel.class)) {
            goToSetupSignalTypeLevel(user, msg);
            EmojiCallback.back(msg.getCallbackId());
        } else if (currCreationLvl.getClass().equals(SetupSignalInversionLevel.class)) {
            goToSetupGPIOLevel(user, msg);
            EmojiCallback.back(msg.getCallbackId());
        }
    }

    void process(IncomingMessage msg) {
        if (currCreationLvl.getClass().equals(SetupNameLevel.class)) {
            String deviceName = (String) currCreationLvl.process(user, msg);

            if (deviceName != null && !deviceName.isEmpty()) {
                creationOutput.setName(deviceName);
                delete(user.getChatId(), user.getLastMessageId());
                user.setLastMessageId(0);
                goToSetupSignalTypeLevel(user, msg);
                user.getDeviceCreator().setCurrCreationLvl(SetupSignalTypeLevel.getInstance());
            }
        } else if (currCreationLvl.getClass().equals(SetupSignalTypeLevel.class)) {
            String signalType = (String) currCreationLvl.process(user, msg);
            if (signalType != null) {
                creationOutput.setType(signalType);
                goToSetupGPIOLevel(user, msg);
                user.getDeviceCreator().setCurrCreationLvl(SetupGPIOLevel.getInstance());
                EmojiCallback.next(msg.getCallbackId());
            }
        } else if (currCreationLvl.getClass().equals(SetupGPIOLevel.class)) {
            Integer gpio = (Integer) currCreationLvl.process(user, msg);
            if (gpio != null) {
                creationOutput.setGpio(gpio);
                goToSetupSignalInversionLevel(user, msg);
                user.getDeviceCreator().setCurrCreationLvl(SetupSignalInversionLevel.getInstance());
                EmojiCallback.next(msg.getCallbackId());
            }
        } else if (currCreationLvl.getClass().equals(SetupSignalInversionLevel.class)) {
            Boolean inversion = (Boolean) currCreationLvl.process(user, msg);
            if (inversion != null) {
                creationOutput.setReverse(inversion);
                createDevice(msg);
                destroy();
                EmojiCallback.success(msg.getCallbackId());
            }
        }
    }


    private void createDevice(IncomingMessage msg) {
        try {
            createOutput(getChannel(user.getChatId()), creationOutput);
            goToDevicesLevel(user, msg);
            EmojiCallback.success(msg.getCallbackId());
        } catch (ChannelNotFoundException e) {
            log.warn(e.getMessage());
            goToHomeControlLevel(user, msg);
        } finally {
            destroy();
        }
    }

    void destroy() {
        user.setDeviceCreator(null);
    }
}
