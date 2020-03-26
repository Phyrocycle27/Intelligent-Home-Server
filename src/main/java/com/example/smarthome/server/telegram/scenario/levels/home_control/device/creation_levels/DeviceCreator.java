package com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels;

import com.example.smarthome.server.entity.Output;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.telegram.CallbackAction;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.scenario.MessageProcessor;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.example.smarthome.server.connection.ClientAPI.createOutput;
import static com.example.smarthome.server.connection.ClientAPI.getChannel;
import static com.example.smarthome.server.telegram.MessageExecutor.deleteAsync;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.HomeControlLevel.goToHomeControlLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.DevicesLevel.goToDevicesLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.SetupGPIOLevel.goToSetupGPIOLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.SetupNameLevel.goToSetupNameLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.SetupSignalInversionLevel.goToSetupSignalInversionLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.SetupSignalTypeLevel.goToSetupSignalTypeLevel;

public class DeviceCreator {

    private static final Logger log = LoggerFactory.getLogger(DeviceCreator.class);

    @Setter(value = AccessLevel.PACKAGE)
    @Getter(value = AccessLevel.PACKAGE)
    private MessageProcessor currCreationLvl;
    private final UserInstance user;

    @Getter(value = AccessLevel.PACKAGE)
    private final Output creationOutput;

    DeviceCreator(UserInstance user) {
        creationOutput = new Output();
        this.user = user;
    }

    void start(IncomingMessage msg, CallbackAction action) {
        goToSetupNameLevel(user, msg, () -> {
            user.setCurrentLvl(DeviceCreationLevel.getInstance());
            user.getDeviceCreator().setCurrCreationLvl(SetupNameLevel.getInstance());
            if (action != null) action.process();
        });
    }

    void goToPrev(IncomingMessage msg, CallbackAction action) {
        if (currCreationLvl.getClass().equals(SetupNameLevel.class)) {
            goToDevicesLevel(user, msg, () -> {
               destroy();
               action.process();
            });
        } else if (currCreationLvl.getClass().equals(SetupSignalTypeLevel.class)) {
            goToSetupNameLevel(user, msg, () -> {
                user.getDeviceCreator().setCurrCreationLvl(SetupNameLevel.getInstance());
                action.process();
            });
        } else if (currCreationLvl.getClass().equals(SetupGPIOLevel.class)) {
            goToSetupSignalTypeLevel(user, msg, () -> {
                user.getDeviceCreator().setCurrCreationLvl(SetupSignalTypeLevel.getInstance());
                action.process();
            });
        } else if (currCreationLvl.getClass().equals(SetupSignalInversionLevel.class)) {
            goToSetupGPIOLevel(user, msg, () -> {
                user.getDeviceCreator().setCurrCreationLvl(SetupGPIOLevel.getInstance());
                action.process();
            });
        }
    }

    void process(IncomingMessage msg) {
        if (currCreationLvl.getClass().equals(SetupNameLevel.class)) {
            String deviceName = (String) currCreationLvl.process(user, msg);

            if (deviceName != null && !deviceName.isEmpty()) {
                creationOutput.setName(deviceName);
                deleteAsync(user.getChatId(), user.getLastMessageId(), () -> user.setLastMessageId(0));
                goToSetupSignalTypeLevel(user, msg, () -> user.getDeviceCreator()
                        .setCurrCreationLvl(SetupSignalTypeLevel.getInstance()));
            }
        } else if (currCreationLvl.getClass().equals(SetupSignalTypeLevel.class)) {
            String signalType = (String) currCreationLvl.process(user, msg);
            if (signalType != null) {
                creationOutput.setType(signalType);
                goToSetupGPIOLevel(user, msg, () -> {
                    user.getDeviceCreator().setCurrCreationLvl(SetupGPIOLevel.getInstance());
                    EmojiCallback.next(msg.getCallbackId());
                });
            }
        } else if (currCreationLvl.getClass().equals(SetupGPIOLevel.class)) {
            Integer gpio = (Integer) currCreationLvl.process(user, msg);
            if (gpio != null) {
                creationOutput.setGpio(gpio);
                goToSetupSignalInversionLevel(user, msg, () -> {
                    user.getDeviceCreator().setCurrCreationLvl(SetupSignalInversionLevel.getInstance());
                    EmojiCallback.next(msg.getCallbackId());
                });
            }
        } else if (currCreationLvl.getClass().equals(SetupSignalInversionLevel.class)) {
            Boolean inversion = (Boolean) currCreationLvl.process(user, msg);
            if (inversion != null) {
                creationOutput.setReverse(inversion);
                createDevice(msg);
                destroy();
            }
        }
    }


    private void createDevice(IncomingMessage msg) {
        try {
            createOutput(getChannel(user.getChatId()), creationOutput);
            goToDevicesLevel(user, msg, () -> EmojiCallback.success(msg.getCallbackId()));
        } catch (ChannelNotFoundException e) {
            log.warn(e.getMessage());
            goToHomeControlLevel(user, msg, null);
        } finally {
            destroy();
        }
    }

    void destroy() {
        user.setDeviceCreator(null);
    }
}
