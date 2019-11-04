package com.example.smarthome.server.telegram;

import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.exceptions.UserAlreadyExistsException;
import com.example.smarthome.server.service.DeviceAccessService;
import io.netty.channel.Channel;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

class UserInstance {
    /* ***************************** MAIN ******************************************************* */
    private static final String defaultMain = "Команды '%s' не существует. Пожалуйста, " +
            "наберите команду \"меню\" чтобы воспользоваться ботом";
    /* ********************************* DEFAULT IN SECTION ************************************* */
    private static final String defaultSection = "Команды '%s' нет в данном разделе. Пожалуйста, " +
            "выберите один из предложенных вариантов";
    /* ************************************* MENU *********************************************** */
    private static final String menuMsg = "Нажмите \"Управление домом\" чтобы перейти к управлению умным домом и " +
            "просмотру информации с датчиков или нажмите \"Информация\", чтобы узнать точное время или погоду";
    private static final List<String> menuButtons;
    /* ************************************* INFO ************************************************ */
    private static final String infoMsg = "Выберите \"Погода\" чтобы узнать погоду в совём городе " +
            "или нажмите \"Время\"чтобы узнать точное время в вашем городе";
    private static final List<String> infoButtons;
    /* ********************************** NOTHING ************************************************ */
    private static final String nothingToShow = "Тут пока ничего нет...";
    /* *********************************** HOME CONTROL *************************************** */
    private static final String tokenNotFound = "Похоже, что у Вас нет токена...\nЧтобы управлять домом через этого телеграм " +
            "бота Вам нужен уникальный токен, который Ваша Raspberry PI будет использовать для подключения у серверу";
    private static final List<String> tokenBtn;
    private static final String channelNotFound = "Ваша Raspberry PI не подключена к серверу\n" +
            "Введите, пожалуйста, свой токен в соответствующем разделе в приложении чтобы Ваше устройство могло " +
            "подключиться к серверу";
    private static final Logger LOGGER;
    private static final YandexWeather weatherService;
    private static final DeviceAccessService service;

    static {
        LOGGER = Logger.getLogger(Bot.class.getName());
        weatherService = new YandexWeather();
        service = DeviceAccessService.getInstance();

        menuButtons = new ArrayList<String>() {{
            add("Управление домом");
            add("Информация");
        }};

        infoButtons = new ArrayList<String>() {{
            add("Погода");
            add("Время");
        }};

        tokenBtn = new ArrayList<String>() {{
            add("Сгенерировать токен");
        }};
    }

    private int level = 0;
    private int subLevel = 0;
    private long userId;

    UserInstance(long userId) {
        this.userId = userId;
    }

    List<SendMessage> getMessage(String incoming) {
        List<SendMessage> messages = new ArrayList<>();

        if (incoming.equals("меню") || incoming.equals("/start")) {
            messages.add(getKeyboard(menuMsg, menuButtons, userId, false));
            level = 1;
            return messages;
        } else if (level == 0) {
            messages.add(createMsg(userId).setText(String.format(defaultMain, incoming)));
        }

        switch (level) {
            case 1:
                switch (incoming) {
                    case "управление домом":
                        if (service.isExists(userId)) {
                            try {
                                Channel ch = service.getChannel(userId);
                                messages.add(getKeyboard(nothingToShow, null, userId, true));
                            } catch (ChannelNotFoundException e) {
                                LOGGER.log(Level.WARNING, e.getMessage());
                                messages.add(getKeyboard(channelNotFound, null, userId, true));
                            }
                        }
                        else
                            messages.add(getKeyboard(tokenNotFound, tokenBtn, userId, true));
                        subLevel = 1;
                        level++;
                        break;
                    case "информация":
                        messages.add(getKeyboard(infoMsg, infoButtons, userId, true));
                        subLevel = 2;
                        level++;
                        break;
                    default:
                        messages.add(createMsg(userId).setText(String.format(defaultSection, incoming)));
                }
                break;
            case 2:
                switch (subLevel) {
                    case 1:
                        switch (incoming) {
                            case "сгенерировать токен":
                                try {
                                    messages.add(createMsg(userId)
                                            .setText("Ваш токен успешно сгенерирован!\n" +
                                                    "Он требуется для подключения Вашего устройства к серверу.\n" +
                                                    "Пожалуйста, скопируйте и вставьте Ваш токен в соответствующий раздел в"+
                                                    "приложении, чтобы Ваше устройство могло подключиться к серверу\n\n" +
                                                    "\u2B07\u2B07\u2B07 ВАШ ТОКЕН \u2B07\u2B07\u2B07"));
                                    // Сообщение с токеном
                                    messages.add(createMsg(userId)
                                            .setText(service.createToken(userId)));

                                } catch (UserAlreadyExistsException e) {
                                    LOGGER.log(Level.WARNING, e.getMessage());
                                    messages.clear();

                                    messages.add(createMsg(userId)
                                            .setText("Вам уже выдан токен. Введите его, пожалуйста, в соответствующем разделе " +
                                                    "в приложении чтобы Ваше устройство могло подключиться к серверу"));
                                }
                                break;
                            case "назад":
                                messages.add(getKeyboard(menuMsg, menuButtons, userId, false));
                                subLevel = 0;
                                level--;
                                break;
                            default:
                                messages.add(createMsg(userId).setText(String.format(defaultSection, incoming)));
                        }
                        break;
                    case 2:
                        switch (incoming) {
                            case "погода":
                                String weather = weatherService.getWeather();
                                SendMessage msg = createMsg(userId);

                                if (weather == null) msg.setText("Ошибка при получении погоды");
                                else msg.setText(weather);

                                messages.add(msg);
                                break;
                            case "время":
                                messages.add(createMsg(userId)
                                        .setText(String.format("Химкинское время %s",
                                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))));
                                break;
                            case "назад":
                                messages.add(getKeyboard(menuMsg, menuButtons, userId, false));
                                subLevel = 0;
                                level--;
                                break;
                            default:
                                messages.add(createMsg(userId).setText(String.format(defaultSection, incoming)));
                        }
                        break;
                }
                break;
        }

        return messages;
    }

    private SendMessage createMsg(long chatId) {
        return new SendMessage().setChatId(chatId);
    }

    private SendMessage getKeyboard(String messageText, List<String> buttonsText, long userId, Boolean prevBtn) {
        SendMessage msg = createMsg(userId);

        msg.setText(messageText);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup()
                .setResizeKeyboard(true)
                .setOneTimeKeyboard(false)
                .setSelective(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        if (buttonsText != null) {
            KeyboardRow row = new KeyboardRow();
            for (int i = 1; i <= buttonsText.size(); i++) {
                row.add(buttonsText.get(i - 1));

                if (i % 2 == 0 & i != buttonsText.size()) {
                    keyboard.add(row);
                    row = new KeyboardRow();
                }
            }
            keyboard.add(row);
        }

        if (prevBtn) {
            KeyboardRow row = new KeyboardRow();
            row.add("Назад");
            keyboard.add(row);
        }

        markup.setKeyboard(keyboard);
        msg.setReplyMarkup(markup);

        return msg;
    }
}
