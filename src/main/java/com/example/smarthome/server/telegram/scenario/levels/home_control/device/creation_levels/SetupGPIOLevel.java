package com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels;

import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.MessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static com.example.smarthome.server.connection.ClientAPI.getAvailableOutputs;
import static com.example.smarthome.server.connection.ClientAPI.getChannel;
import static com.example.smarthome.server.telegram.MessageExecutor.execute;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.HomeControlLevel.goToHomeControlLevel;

public class SetupGPIOLevel implements MessageProcessor {

    private static final SetupGPIOLevel instance = new SetupGPIOLevel();

    private static final Logger log = LoggerFactory.getLogger(SetupGPIOLevel.class);

    // ************************************* MESSAGES *************************************************
    private static final String choosePin = "Теперь выберите пин, к которому вы хотите подключить новое устройство";

    private SetupGPIOLevel() {
    }

    public static SetupGPIOLevel getInstance() {
        return instance;
    }

    @Override
    public Object process(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {
            try {
                if (getAvailableOutputs(getChannel(user.getChatId()), user.getDeviceCreator()
                        .getCreationOutput().getType()).contains(msg.getText())) {
                    return Integer.valueOf(msg.getText());
                }
            } catch (NumberFormatException e) {
                log.error(e.getMessage());
            } catch (ChannelNotFoundException e) {
                log.warn(e.getMessage());
                goToHomeControlLevel(user, msg);
                user.getDeviceCreator().destroy();
            }
        }
        return null;
    }

    public static void goToSetupGPIOLevel(UserInstance user, IncomingMessage msg) {
        try {
            execute(new InlineKeyboardMessage(user.getChatId(), choosePin, new ArrayList<CallbackButton>() {{
                for (String s : getAvailableOutputs(getChannel(user.getChatId()), user.getDeviceCreator()
                        .getCreationOutput().getType()))
                    add(new CallbackButton(s, s));
            }})
                    .setMessageId(msg.getId())
                    .setNumOfColumns(6)
                    .hasBackButton(true));
        } catch (ChannelNotFoundException e) {
            log.warn(e.getMessage());
            goToHomeControlLevel(user, msg);
            user.getDeviceCreator().destroy();
        }
    }
}
