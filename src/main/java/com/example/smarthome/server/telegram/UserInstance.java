package com.example.smarthome.server.telegram;

import com.example.smarthome.server.connection.JsonRequester;
import com.example.smarthome.server.entity.Output;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.service.DeviceAccessService;
import io.netty.channel.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

class UserInstance {
    // ***************************** MAIN *******************************************************
    private static final String unknownCmdMain = "Команды '%s' не существует. Пожалуйста, " +
            "наберите команду \"меню\" чтобы воспользоваться ботом";

    // ********************************* DEFAULT IN SECTION *************************************
    private static final String defaultSection = "Команды '%s' нет в данном разделе. Пожалуйста, " +
            "выберите один из предложенных вариантов";

    // ************************************* MENU ***********************************************
    private static final String menuMsg = "Нажмите \"Управление домом\" чтобы перейти к управлению умным домом и " +
            "просмотру информации с датчиков или нажмите \"Информация\", чтобы узнать точное время или погоду";
    private static final String[] menuButtons = new String[]{"Управление домом", "Информация"};

    // ************************************* INFO ************************************************
    private static final String infoMsg = "Выберите \"Погода\" чтобы узнать погоду в совём городе " +
            "или нажмите \"Время\"чтобы узнать точное время в вашем городе";
    private static final String[] infoButtons = new String[]{"Погода", "Время"};

    // *********************************** HOME CONTROL ***************************************
    private static final String channelNotFound = "Ваша Raspberry PI не подключена к серверу\n" +
            "Введите, пожалуйста, свой токен в соответствующем разделе в приложении чтобы Ваше устройство могло " +
            "подключиться к серверу";
    private static final String homeControl = "Выберите Устройства, чтобы управлять устройствами или добавить новое, или " +
            "выберите Датчики чтобы посмотреть показания или добавить новый датчик";
    private static final String[] typesOfSignal = new String[]{"Цифровой", "ШИМ"};
    private static final String[] yesOrNo = new String[]{"Да", "Нет"};
    private static final String[] onOrOff = new String[]{"Включить", "Выключить"};
    private static final String[] homeControlBtns = new String[]{"Устройства", "Датчики"};
    // ****************************************** TOKEN ***********************************************************
    private static final String tokenAlreadyHaveGot = "Похоже, Ваша Raspberry PI не подключена к серверу";
    private static final String tokenSuccessGen = "Ваш токен успешно сгенерирован!\nОн требуется для подключения Вашей " +
            "Raspberry PI к серверу.\nПожалуйста, скопируйте и вставьте Ваш токен в соответствующий раздел в приложении." +
            "\n\n\u2B07\u2B07\u2B07 ВАШ ТОКЕН \u2B07\u2B07\u2B07";
    private static final String tokenNotFound = "Похоже, что у Вас нет токена...\nЧтобы управлять домом через этого телеграм " +
            "бота Вам нужен уникальный токен, который Ваша Raspberry PI будет использовать для подключения у серверу";
    private static final String[] tokenGenBtn = new String[]{"Сгенерировать токен"};

    // ******************************** STATIC FINAL VARIABLES **********************************************
    private static final Logger LOGGER;
    private static final Weather weatherService;
    private static final DeviceAccessService service;

    static {
        LOGGER = Logger.getLogger(Bot.class.getName());
        weatherService = new Weather();
        service = DeviceAccessService.getInstance();
    }

    /* ************************************ TEMP VARIABLES ************************************************** */
    private Map<String, Integer> outputsMap;
    private DeviceCreator creator;

    private int level = 0;
    private int subLevel = 0;
    private int outputId;
    private long userId;

    UserInstance(long userId) {
        this.userId = userId;
    }

    List<SendMessage> getMessage(String incoming) {
        List<SendMessage> messages = new ArrayList<>();

        // если пользователь нам прислал команду "меню" или "/start", то отправляем ему главное меню
        if (incoming.toLowerCase().equals("меню") || incoming.equals("/start")) {
            messages.add(getKeyboard(menuMsg, menuButtons, userId, 2, false,
                    false, false));
            level = 1;
            return messages;
        } else if (level == 0) {
            messages.add(createMsg(userId).setText(String.format(unknownCmdMain, incoming)));
            return messages;
        }
        // а вот тут-то мы и прорабатываем весь сценарий бота и его команды
        switch (level) {
            /* ***********************************
             __________________   ____________
            | Управление домом | | Информация |
             ¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯   ¯¯¯¯¯¯¯¯¯¯¯¯
            *********************************** */
            case 1:
                switch (incoming) {
                    case "Управление домом":
                        if (service.isExists(userId)) {
                            messages.add(goToHomeControlLevel());
                        } else {
                            messages.add(getKeyboard(tokenNotFound, tokenGenBtn, userId, 2,
                                    true, false, false));
                            subLevel = 1;
                            level = 2;
                        }
                        break;
                    case "Информация":
                        messages.add(getKeyboard(infoMsg, infoButtons, userId, 2, true,
                                false, false));
                        subLevel = 2;
                        level = 2;
                        break;
                    default:
                        messages.add(createMsg(userId).setText(String.format(defaultSection, incoming)));
                }
                break;
            case 2:
                if (incoming.equals("Назад")) {
                    messages.add(getKeyboard(menuMsg, menuButtons, userId, 2,
                            false, false, false));
                    subLevel = 0;
                    level = 1;
                }
                switch (subLevel) {
                    /* *******************************************************************************
                     _____________________   _____________________           _____________________
                    |     Устройства      | |       Датчики       |         | Сгенерировать токен |
                     ¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯   ¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯    OR     ¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯
                     _____________________________________________           _____________________
                    |                    Назад                    |         |        Назад        |
                     ¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯           ¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯
                    ***************************************************************************** */
                    case 1:
                        switch (incoming) {
                            case "Устройства":
                                messages.add(goToDevices(null));
                                break;
                            case "Датчики":
                                // Запрашиваем с raspberry pi все подключенные датчики и выводим их как кнопки
                                List<String> inputsBtns = new ArrayList<>();

                                messages.add(getKeyboard("Нажмите на существующий датчик или добавьте новый",
                                        (String[]) inputsBtns.toArray(), userId, 2, true,
                                        true, false));
                                level = 3;
                                subLevel = 2;
                                break;
                            case "Сгенерировать токен":
                                if (!service.isExists(userId)) {

                                    messages.add(getKeyboard(tokenSuccessGen, null, userId, 2,
                                            true, false, false));

                                    // Сообщение с токеном
                                    messages.add(createMsg(userId)
                                            .setText(service.createToken(userId)));
                                } else
                                    messages.add(createMsg(userId)
                                            .setText(tokenAlreadyHaveGot));

                                break;
                            default:
                                messages.add(createMsg(userId).setText(String.format(defaultSection, incoming)));
                        }
                        break;

                    /* ************************
                       ________   ________
                      | Время  | | Погода |
                       ¯¯¯¯¯¯¯¯   ¯¯¯¯¯¯¯¯
                       ___________________
                      |        Назад      |
                       ¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯
                     ******************** */
                    case 2:
                        switch (incoming) {
                            case "Погода":
                                String weather = weatherService.getWeather();
                                SendMessage msg = createMsg(userId);

                                if (weather == null) msg.setText("Ошибка при получении погоды");
                                else msg.setText(weather);

                                messages.add(msg);
                                break;
                            case "Время":
                                messages.add(createMsg(userId)
                                        .setText(String.format("Химкинское время %s",
                                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))));
                                break;
                            default:
                                messages.add(createMsg(userId).setText(String.format(defaultSection, incoming)));
                        }
                        break;
                }
                break;
            case 3:
                if (incoming.equals("Назад")) {
                    messages.add(goToHomeControlLevel());
                    outputsMap = null;
                    break;
                }
                switch (subLevel) {
                    // Устройства
                    case 1:
                        if (incoming.equals("Добавить")) {
                            messages.add(creator.goToStepOne());
                            creator = new DeviceCreator();
                        } else if (outputsMap.containsValue(Integer.parseInt(incoming))) {
                            try {
                                Output output = getOutput(Integer.valueOf(incoming));

                                if (output == null)
                                    messages.add(goToDevices("Устройство не найдено"));
                                else {
                                    String currState = getDigitalState(output.getOutputId()) ? "включено" : "выключено";
                                    String inversion = output.getReverse() ? "включена" : "выключена";
                                    String signalType = "";
                                    switch (output.getType()) {
                                        case "digital":
                                            signalType = "цифовой";
                                            break;
                                        case "pwm":
                                            signalType = "ШИМ";
                                    }

                                    messages.add(getKeyboard(String.format("<b>%s</b>\n" +
                                                    "Текущее состояние: <i>%s</i>\n" +
                                                    "Тип сигнала: <i>%s</i>\n" +
                                                    "Инверсия: <i>%s</i>\n" +
                                                    "GPIO-пин: <i>%d</i>",
                                            output.getName(), currState, signalType, inversion,
                                            output.getGpio()), onOrOff, userId, 2, true,
                                            false, true));
                                    level = 4;
                                    subLevel = 2;
                                    outputId = output.getOutputId();
                                }

                            } catch (ChannelNotFoundException e) {
                                goToHomeControlLevel();
                                e.printStackTrace();
                            }
                        } else {
                            messages.add(createMsg(userId).setText(String.format(defaultSection, incoming)));
                        }
                        break;
                    // Датчики
                }
                break;
            case 4:
                switch (subLevel) {
                    case 1:
                        if (incoming.equals("Назад")) {
                            messages.add(creator.goToPrev());
                            break;
                        }

                        switch (creator.getStep()) {
                            // Вот тут мы указываем имя нового пина
                            case 1:
                                messages.add(creator.setOutputName(incoming));
                                break;
                            // Теперь принимаем от пользователя тип сигнала устройства
                            case 2:
                                messages.add(creator.setOutputSignalType(incoming));
                                break;
                            // Тут мы пишем пин, к которому подключено устройство
                            case 3:
                                messages.add(creator.setOutputGpio(incoming));
                                break;
                            // А тут мы (не)устанавливаем инверсию сигнала для данного устройства
                            case 4:
                                messages.add(creator.setOutputReverse(incoming));
                                creator = null;
                                break;
                        }
                        break;
                    case 2:
                        if (incoming.equals("Включить")) {
                            try {
                                messages.add(createMsg(userId).setText("Устройство успешно включено"));
                                setDigitalState(outputId, true);
                            } catch (ChannelNotFoundException e) {
                                e.printStackTrace();
                            }
                        } else if (incoming.equals("Выключить")) {
                            try {
                                messages.add(createMsg(userId).setText("Устройство успешно выключено"));
                                setDigitalState(outputId, false);
                            } catch (ChannelNotFoundException e) {
                                e.printStackTrace();
                            }
                        } else {
                            switch (incoming) {
                                case "Удалить":
                                    try {
                                        deleteOutput(outputId);
                                        messages.add(goToDevices("Устройство успешно удалено"));
                                    } catch (ChannelNotFoundException e) {
                                        goToHomeControlLevel();
                                        e.printStackTrace();
                                    }
                                    break;
                                case "Назад":
                                    messages.add(goToDevices(null));
                                    outputId = 0;
                                    break;
                                default:
                                    messages.add(createMsg(userId).setText(defaultSection));
                            }
                        }
                }
                break;
        }

        return messages;
    }

    // LEVELS
    private SendMessage goToHomeControlLevel() {
        SendMessage msg;
        if (service.isChannelExist(userId)) {
            msg = getKeyboard(homeControl, homeControlBtns, userId, 2, true,
                    false, false);
            level = 2;
            subLevel = 1;
        } else {
            msg = getKeyboard(channelNotFound, menuButtons, userId, 2, false,
                    false, false);
            level = 1;
            subLevel = 0;
        }

        return msg;
    }

    private SendMessage goToDevices(String text) {
        SendMessage answer;
        try {
            if (outputsMap == null)
                outputsMap = new HashMap<>();

            for (Output output : getOutputs()) {
                if (!outputsMap.containsKey(output.getName()))
                    outputsMap.put(output.getName(), output.getOutputId());
            }

            Map<String, String> map = new HashMap<>();
            for (Map.Entry<String, Integer> entry : outputsMap.entrySet())
                map.put(entry.getKey(), String.valueOf(entry.getValue()));

            String answerText;
            if (text == null)
                answerText = "Нажмите на существующее устройство или добавьте новое";
            else answerText = text;

            answer = getInlineKeyboard(answerText,
                    map, 3);

            level = 3;
            subLevel = 1;
        } catch (ChannelNotFoundException e) {
            LOGGER.log(Level.WARNING, e.getMessage());
            answer = goToHomeControlLevel();
        }

        return answer;
    }

    // ПОЛУЧЕНИЕ И ОТПРАВКА ДАННЫХ С Raspberry PI

    /* **************************************************************************************************************
     ************************* ПОЛУЧЕНИЕ И ОТПРАВКА ДАННЫХ С Raspberry PI *******************************************
     ************************************************************************************************************** */

    private boolean getDigitalState(@NonNull Integer outputId) throws ChannelNotFoundException {
        JSONObject request = new JSONObject()
                .put("type", "request")
                .put("body", new JSONObject()
                        .put("method", "GET")
                        .put("uri", "http://localhost:8080/outputs/control/digital?id=" + outputId));

        return JsonRequester.execute(request, getChannel()).getJSONObject("body").getBoolean("digitalState");
    }

    private void setDigitalState(@NonNull Integer outputId, boolean state) throws ChannelNotFoundException {
        JSONObject request = new JSONObject()
                .put("type", "request")
                .put("body", new JSONObject()
                        .put("method", "PUT")
                        .put("uri", "http://localhost:8080/outputs/control/digital")
                        .put("request_body", new JSONObject()
                                .put("outputId", outputId)
                                .put("digitalState", state)));

        JsonRequester.execute(request, getChannel());
    }

    // DELETE
    private void deleteOutput(Integer outputId) throws ChannelNotFoundException {
        JSONObject request = new JSONObject()
                .put("type", "request")
                .put("body", new JSONObject()
                        .put("method", "DELETE")
                        .put("uri", "http://localhost:8080/outputs/one/" + outputId));

        JsonRequester.execute(request, getChannel());
    }

    // CREATE
    private void createOutput(Output newOutput) throws ChannelNotFoundException {
        JSONObject request = new JSONObject()
                .put("type", "request")
                .put("body", new JSONObject()
                        .put("method", "POST")
                        .put("uri", "http://localhost:8080/outputs")
                        .put("request_body", new JSONObject(newOutput)));

        JSONObject response = JsonRequester.execute(request, getChannel())
                .getJSONObject("body");
    }

    // GET
    private Output getOutput(Integer outputId) throws ChannelNotFoundException {
        Output output = new Output();

        // ***********************************************************
        JSONObject request = new JSONObject()
                .put("type", "request")
                .put("body", new JSONObject()
                        .put("method", "GET")
                        .put("uri", "http://localhost:8080/outputs/one/" + outputId));
        // ***********************************************************

        JSONObject response = JsonRequester.execute(request, getChannel())
                .getJSONObject("body");

        if (response.getInt("code") == 200) {
            output.setOutputId(response.getInt("outputId"));
            output.setName(response.getString("name"));
            output.setGpio(response.getInt("gpio"));
            output.setReverse(response.getBoolean("reverse"));
            output.setType(response.getString("type"));
            output.setCreationDate(LocalDateTime.parse(
                    response.getString("creationDate"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            return output;
        } else {
            // удаляем это выход из нашего hashMap
            return null;
        }
    }

    // GET LIST
    private List<Output> getOutputs() throws ChannelNotFoundException {
        List<Output> outputs = new ArrayList<>();

        // ***********************************************************
        JSONObject reqest = new JSONObject()
                .put("type", "request")
                .put("body", new JSONObject()
                        .put("method", "GET")
                        .put("uri", "http://localhost:8080/outputs/all"));
        // ***********************************************************
        JSONObject response = JsonRequester.execute(reqest, getChannel())
                .getJSONObject("body");

        if (response.keySet().contains("_embedded")) {
            // Создаём из объектов массива JSON объекты Output
            // и вносим их в List outputs
            JSONArray array = response.getJSONObject("_embedded")
                    .getJSONArray("outputList");
            for (int i = 0; i < array.length(); i++) {
                JSONObject outputJson = array.getJSONObject(i);
                Output output = new Output();

                output.setName(outputJson.getString("name"));
                output.setOutputId(outputJson.getInt("outputId"));

                outputs.add(output);
            }
        }
        return outputs;
    }

    // GET LIST CONTAINS NUMBERS OF FREE GPIOS
    private String[] getAvailableOutputs(String type) throws ChannelNotFoundException {
        String[] gpios;

        JSONObject request = new JSONObject()
                .put("type", "request")
                .put("body", new JSONObject()
                        .put("method", "GET")
                        .put("uri", "http://localhost:8080/outputs/available?type=" + type));

        JSONArray array = JsonRequester.execute(request, getChannel())
                .getJSONObject("body")
                .getJSONArray("available_gpios");

        gpios = (String[]) array.toList().toArray();
        return gpios;
    }

    private Channel getChannel() throws ChannelNotFoundException {
        return service.getChannel(userId);
    }

    // **************************************************************************************************************

    private SendMessage createMsg(long chatId) {
        return new SendMessage().setChatId(chatId).setParseMode("HTML");
    }

    private SendMessage getKeyboard(String messageText, String[] buttons, long userId, int numOfColumns,
                                    boolean prevBtn, boolean addBtn, boolean removeBtn) {

        SendMessage msg = createMsg(userId);

        if (messageText != null)
            msg.setText(messageText);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup()
                .setResizeKeyboard(true)
                .setOneTimeKeyboard(false)
                .setSelective(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        if (buttons != null) {
            KeyboardRow row = new KeyboardRow();
            for (int i = 1; i <= buttons.length; i++) {
                row.add(buttons[i - 1]);

                if (i % numOfColumns == 0 & i != buttons.length) {
                    keyboard.add(row);
                    row = new KeyboardRow();
                }
            }
            keyboard.add(row);
        }

        KeyboardRow row = new KeyboardRow();

        if (removeBtn)
            row.add("Удалить");
        keyboard.add(row);
        row = new KeyboardRow();

        if (prevBtn)
            row.add("Назад");

        if (addBtn)
            row.add("Добавить");

        keyboard.add(row);

        markup.setKeyboard(keyboard);
        msg.setReplyMarkup(markup);

        return msg;
    }

    private SendMessage getInlineKeyboard(String messageText, Map<String, String> buttons, int numOfColumns) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        int i = 0;

        for (Map.Entry<String, String> entry : buttons.entrySet()) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText(entry.getKey()).setCallbackData(entry.getValue());
            row.add(btn);
            i++;
            if (i % numOfColumns == 0) {
                rowList.add(row);
                row = new ArrayList<>();
            }
        }
        rowList.add(row);
        markup.setKeyboard(rowList);

        return createMsg(userId).setText(messageText).setReplyMarkup(markup);
    }

    /* **************************************************************************************************************
     ************************************** СОЗДАНИЕ УСТРОЙСТВА *****************************************************
     ************************************************************************************************************** */

    private class DeviceCreator {

        @Getter(value = AccessLevel.PRIVATE)
        private Output creationOutput;

        @Getter(value = AccessLevel.PRIVATE)
        @Setter(value = AccessLevel.PRIVATE)
        private int step = 0;

        private DeviceCreator() {
            creationOutput = new Output();
        }

        private SendMessage goToPrev() {
            switch (step) {
                case 1:
                    return goToDevices(null);
                case 2:
                    return goToStepOne();
                case 3:
                    return goToStepTwo();
                case 4:
                    return goToStepThree(creationOutput.getType());
            }
            return null;
        }

        private SendMessage goToStepOne() {
            SendMessage msg = getKeyboard("Пожалуйста, введите имя нового устройства:",
                    null, userId, 2, true, false, false);
            level = 4;
            subLevel = 1;
            step = 1;
            return msg;
        }

        // Step one - SET UP DEVICE NAME
        private SendMessage setOutputName(String name) {
            creationOutput.setName(name);
            return goToStepTwo();
        }

        private SendMessage goToStepTwo() {
            SendMessage msg = getKeyboard("Отлично! Теперь наберите тип сигнала, который " +
                            "может принимать устройство", typesOfSignal, userId, 2,
                    true, false, false);
            step = 2;
            return msg;
        }

        private SendMessage setOutputSignalType(String signalType) {
            SendMessage msg;
            switch (signalType) {
                case "цифровой":
                    creationOutput.setType("digital");
                    msg = goToStepThree("digital");
                    break;
                case "шим":
                    creationOutput.setType("pwm");
                    msg = goToStepThree("pwm");
                    break;
                default:
                    msg = createMsg(userId).setText("Вы ввели несуществующий тип сигнала");
            }
            return msg;
        }

        private SendMessage goToStepThree(String signalType) {
            SendMessage msg = new SendMessage();
            try {
                switch (signalType) {
                    case "digital":
                        msg = getKeyboard("Теперь выберите пин, к которому вы хотите подключить новое " +
                                        "устройство", getAvailableOutputs("digital"), userId, 4,
                                true, false, false);
                        step = 3;
                        break;
                    case "pwm":
                        msg = getKeyboard("Теперь выберите пин, к которому вы хотите подключить новое " +
                                        "устройство", getAvailableOutputs("pwm"), userId, 4,
                                true, false, false);
                        step = 3;
                        break;
                }
            } catch (ChannelNotFoundException e) {
                LOGGER.log(Level.WARNING, e.getMessage());
                msg = goToHomeControlLevel();
                creationOutput = null;
            }
            return msg;
        }

        private SendMessage setOutputGpio(String gpioStr) {
            SendMessage msg;
            try {
                if (contains(gpioStr, getAvailableOutputs(creationOutput.getType()))) {
                    creationOutput.setGpio(Integer.valueOf(gpioStr));

                    msg = goToStepFour();
                } else
                    msg = getKeyboard("Выберите предложенный в списке выход",
                            getAvailableOutputs(creationOutput.getType()), userId, 4, true,
                            false, false);

            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, e.getMessage());
                msg = createMsg(userId).setText("Вы ввели не число");
            } catch (ChannelNotFoundException e) {
                LOGGER.log(Level.WARNING, e.getMessage());
                msg = goToHomeControlLevel();
            }
            return msg;
        }

        private SendMessage goToStepFour() {
            SendMessage msg = getKeyboard("Сделать инверсию сигнала для данного устройства?",
                    yesOrNo, userId, 2, true,
                    false, false);
            step = 4;
            return msg;
        }

        private SendMessage setOutputReverse(String reverse) {
            switch (reverse) {
                case "да":
                    creationOutput.setReverse(true);
                    break;
                case "нет":
                    creationOutput.setReverse(false);
                    break;
                default:
                    return createMsg(userId).setText("Ответ должен быть либо \"Да\", либо \"Нет\"");

            }
            try {
                createOutput(creationOutput);
                return goToDevices("Устройство успешно создано");
            } catch (ChannelNotFoundException e) {
                LOGGER.log(Level.WARNING, e.getMessage());
                return goToHomeControlLevel();
            } finally {
                creationOutput = null;
                step = 0;
            }
        }

        private boolean contains(String s, String[] arr) {
            return Arrays.binarySearch(arr, s) >= 0;
        }
    }

    // **************************************************************************************************************
}