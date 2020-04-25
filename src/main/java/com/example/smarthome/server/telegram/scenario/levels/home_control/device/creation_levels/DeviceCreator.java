package com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels;

import com.example.smarthome.server.connection.ClientAPI;
import com.example.smarthome.server.entity.Device;
import com.example.smarthome.server.entity.GPIO;
import com.example.smarthome.server.entity.GPIOMode;
import com.example.smarthome.server.entity.GPIOType;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.exceptions.OutputAlreadyExistException;
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

import static com.example.smarthome.server.connection.ClientAPI.getChannel;
import static com.example.smarthome.server.telegram.MessageExecutor.deleteAsync;
import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.HomeControlLevel.goToHomeControlLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.DevicesLevel.goToDevicesLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.SetupGPIOLevel.goToSetupGPIOLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.SetupNameLevel.goToSetupNameLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.SetupSignalInversionLevel.goToSetupSignalInversionLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.SetupSignalTypeLevel.goToSetupSignalTypeLevel;

public class DeviceCreator {

    private static final Logger log = LoggerFactory.getLogger(DeviceCreator.class);
    private static final String deviceExists = "Устройство с таким пином уже существует";

    @Setter(value = AccessLevel.PACKAGE)
    @Getter(value = AccessLevel.PACKAGE)
    private MessageProcessor currCreationLvl;
    private final UserInstance user;

    @Getter(value = AccessLevel.PACKAGE)
    private final Device creationDevice;

    DeviceCreator(UserInstance user) {
        creationDevice = new Device();
        creationDevice.setGpio(new GPIO());
        creationDevice.getGpio().setMode(GPIOMode.OUTPUT);
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

    boolean process(IncomingMessage msg) {
        if (currCreationLvl.getClass().equals(SetupNameLevel.class)) {
            String deviceName = (String) currCreationLvl.process(user, msg);
            if (deviceName != null && !deviceName.isEmpty()) {
                creationDevice.setName(deviceName);
                deleteAsync(user.getChatId(), user.getLastMessageId(), () -> user.setLastMessageId(0));
                goToSetupSignalTypeLevel(user, msg, () -> user.getDeviceCreator()
                        .setCurrCreationLvl(SetupSignalTypeLevel.getInstance()));
                return true;
            }
        } else if (currCreationLvl.getClass().equals(SetupSignalTypeLevel.class)) {
            GPIOType signalType = (GPIOType) currCreationLvl.process(user, msg);
            if (signalType != null) {
                creationDevice.getGpio().setType(signalType);
                goToSetupGPIOLevel(user, msg, () -> {
                    user.getDeviceCreator().setCurrCreationLvl(SetupGPIOLevel.getInstance());
                    EmojiCallback.next(msg.getCallbackId());
                });
                return true;
            }
        } else if (currCreationLvl.getClass().equals(SetupGPIOLevel.class)) {
            Integer gpio = (Integer) currCreationLvl.process(user, msg);
            if (gpio != null) {
                creationDevice.getGpio().setGpio(gpio);
                goToSetupSignalInversionLevel(user, msg, () -> {
                    user.getDeviceCreator().setCurrCreationLvl(SetupSignalInversionLevel.getInstance());
                    EmojiCallback.next(msg.getCallbackId());
                });
                return true;
            }
        } else if (currCreationLvl.getClass().equals(SetupSignalInversionLevel.class)) {
            Boolean inversion = (Boolean) currCreationLvl.process(user, msg);
            if (inversion != null) {
                creationDevice.setReverse(inversion);
                createDevice(msg);
                destroy();
                return true;
            }
        }
        return false;
    }


    private void createDevice(IncomingMessage msg) {
        try {
            ClientAPI.createDevice(getChannel(user.getChatId()), creationDevice);
            goToDevicesLevel(user, msg, () -> EmojiCallback.success(msg.getCallbackId()));
        } catch (OutputAlreadyExistException e) {
            log.warn(e.getMessage());
            goToDevicesLevel(user, msg, () -> executeAsync(new AnswerCallback(msg.getCallbackId(), deviceExists)));
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
