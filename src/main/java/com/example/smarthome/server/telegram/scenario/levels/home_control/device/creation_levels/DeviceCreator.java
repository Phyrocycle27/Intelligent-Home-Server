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
    }

    void goToPrev(IncomingMessage msg) {
        if (currCreationLvl.getClass().equals(SetupNameLevel.class)) {
            goToDevicesLevel(user, msg);
        } else if (currCreationLvl.getClass().equals(SetupSignalTypeLevel.class)) {
            goToSetupNameLevel(user, msg);
        } else if (currCreationLvl.getClass().equals(SetupGPIOLevel.class)) {
            goToSetupSignalTypeLevel(user, msg);
        } else if (currCreationLvl.getClass().equals(SetupSignalInversionLevel.class)) {
            goToSetupGPIOLevel(user, msg);
        }
    }

    void answer(IncomingMessage msg) {
        if (currCreationLvl.getClass().equals(SetupNameLevel.class)) {
            if (currCreationLvl.process(user, msg)) {
                goToSetupSignalTypeLevel(user, msg);
            }
        } else if (currCreationLvl.getClass().equals(SetupSignalTypeLevel.class)) {
            if (currCreationLvl.process(user, msg)) {
                goToSetupGPIOLevel(user, msg);
            }
        } else if (currCreationLvl.getClass().equals(SetupGPIOLevel.class)) {
            if (currCreationLvl.process(user, msg)) {
                goToSetupSignalInversionLevel(user, msg);
            }
        } else if (currCreationLvl.getClass().equals(SetupSignalInversionLevel.class)) {
            if (currCreationLvl.process(user, msg)) {
                createDevice(msg);
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
        creationOutput = null;
        user.setDeviceCreator(null);
    }
}
