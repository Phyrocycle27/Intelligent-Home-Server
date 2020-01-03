package com.example.smarthome.server.telegram;

import com.example.smarthome.server.connection.JsonRequester;
import com.example.smarthome.server.entity.Output;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.service.DeviceAccessService;
import io.netty.channel.Channel;
import lombok.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerShippingQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultGame;
import org.telegram.telegrambots.meta.api.objects.payments.ShippingOption;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

class UserInstance {
    // ***************************** MAIN *******************************************************
    private static final String unknownCmdMain = "Команды '%s' не существует. Пожалуйста, " +
            "наберите команду \"меню\" чтобы воспользоваться ботом";

    // ********************************* DEFAULT IN SECTION *************************************
    private static final String unknownCmdSection = "Команды '%s' нет в данном разделе. Пожалуйста, " +
            "выберите один из предложенных вариантов";

    // ************************************* MENU ***********************************************
    private static final String menuMsg = "Нажмите \"Управление домом\" чтобы перейти к управлению умным домом и " +
            "просмотру информации с датчиков или нажмите \"Информация\", чтобы узнать точное время или погоду";
    private static final List<CallbackButton> menuButtons = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Управление домом", "home_control"));
        add(new CallbackButton("Информация", "information"));
    }};
    // ************************************* INFO ************************************************
    private static final String infoMsg = "Выберите \"Погода\" чтобы узнать погоду в совём городе " +
            "или нажмите \"Время\"чтобы узнать точное время в вашем городе";
    private static final List<CallbackButton> infoButtons = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Погода", "weather"));
        add(new CallbackButton("Время", "time"));
    }};
    // *********************************** HOME CONTROL ***************************************
    private static final String channelNotFound = "Ваша Raspberry PI не подключена к серверу\n" +
            "Введите, пожалуйста, свой токен в соответствующем разделе в приложении чтобы Ваше устройство могло " +
            "подключиться к серверу";
    private static final String homeControl = "Выберите Устройства, чтобы управлять устройствами или добавить новое, или " +
            "выберите Датчики чтобы посмотреть показания или добавить новый датчик";
    private static final String[] typesOfSignal = new String[]{"Цифровой", "ШИМ"};
    private static final String[] yesOrNo = new String[]{"Да", "Нет"};
    private static final String[] onOrOff = new String[]{"Включить", "Выключить"};
    private static final List<CallbackButton> homeControlButtons = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Устройства", "devices"));
        add(new CallbackButton("Датчики", "sensors"));
    }};
    private static final String sensorsMsg = "Нажмите на существующий датчик или добавьте новый";
    private static final String devicesMsg = "Нажмите на существующее устройство или добавьте новое";
    // ****************************************** TOKEN ***********************************************************
    private static final String tokenAlreadyHaveGot = "Похоже, Ваша Raspberry PI не подключена к серверу";
    private static final String tokenSuccessGen = "Ваш токен успешно сгенерирован!\nОн требуется для подключения Вашей " +
            "Raspberry PI к серверу.\nПожалуйста, скопируйте и вставьте Ваш токен в соответствующий раздел в приложении." +
            "\n\n\u21E7\u21E7\u21E7 ВАШ ТОКЕН \u21E7\u21E7\u21E7";
    private static final String tokenNotFound = "Похоже, что у Вас нет токена...\nЧтобы управлять домом через этого телеграм " +
            "бота Вам нужен уникальный токен, который Ваша Raspberry PI будет использовать для подключения у серверу";
    private static final List<CallbackButton> tokenGenButton = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Сгенерировать токен", "token_gen"));
    }};

    // ******************************** STATIC FINAL VARIABLES **********************************************
    public static final Logger log;
    private static final Weather weatherService;
    private static final DeviceAccessService service;
    private static Bot bot;

    /* ************************************ TEMP VARIABLES ************************************************** */
//    private DeviceCreator creator;
    private long chatId;

    // Scenery
    private Consumer<IncomingMessage> currentLvl;
    private Predicate<IncomingMessage> defaultLvl;
    private Consumer<IncomingMessage> menuLvl;
    private Consumer<IncomingMessage> infoLvl;
    private Consumer<IncomingMessage> homeControlLvl;
    private Consumer<IncomingMessage> devicesLvl;
    private Consumer<IncomingMessage> deviceControlLvl;
    private Consumer<IncomingMessage> deviceCreationLvl;

    static {
        log = LoggerFactory.getLogger(Bot.class);
        weatherService = new Weather();
        service = DeviceAccessService.getInstance();
    }

    UserInstance(long chatId) {
        this.chatId = chatId;
        init();
    }

    public static void setBot(Bot bot) {
        UserInstance.bot = bot;
    }

    private void init() {
        defaultLvl = msg -> {
            log.info("Default level");
            if (msg.getText().toLowerCase().equals("меню") || msg.getText().equals("/start")) {
                execute(new InlineKeyboardMessage(chatId, menuMsg, menuButtons)
                        .setMessageId(msg.getId())
                        .setNumOfColumns(2));
                currentLvl = menuLvl;
                return true;
            } else {
                return false;
            }
        };

        menuLvl = msg -> {
            log.info("Menu level");
            switch (msg.getText()) {
                case "home_control":
                    if (service.isExists(chatId))
                        goToHomeControlLevel(msg);
                    else {
                        execute(new InlineKeyboardMessage(chatId, tokenNotFound, tokenGenButton)
                                .setMessageId(msg.getId())
                                .hasBackButton(true));
                        currentLvl = homeControlLvl;
                    }
                    break;
                case "information":
                    execute(new InlineKeyboardMessage(chatId, infoMsg, infoButtons)
                            .setMessageId(msg.getId())
                            .setNumOfColumns(2)
                            .hasBackButton(true));
                    currentLvl = infoLvl;
                    break;
                default:
                    if (!msg.getCallbackId().isEmpty()) {
                        execute(new AnswerCallback(msg.getCallbackId(), "Кнопка недействительна"));
                        delete(msg.getId());
                    }
            }
        };

        infoLvl = msg -> {
            log.info("Info level");
            switch (msg.getText()) {
                case "weather":
                    String weather = weatherService.getWeather();
                    String answer;

                    if (weather == null) answer = "Ошибка при получении погоды";
                    else answer = weather;

                    try {
                        execute(new InlineKeyboardMessage(chatId, answer, infoButtons)
                                .setMessageId(msg.getId())
                                .setNumOfColumns(2)
                                .hasBackButton(true));
                    } catch (RuntimeException e) {
                        log.error(e.getMessage());
                        execute(new AnswerCallback(msg.getCallbackId(), "\u2705"));
                    }
                    break;
                case "time":
                    try {
                        execute(new InlineKeyboardMessage(chatId, String.format("Химкинское время %s",
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))), infoButtons)
                                .setMessageId(msg.getId())
                                .setNumOfColumns(2)
                                .hasBackButton(true));
                    } catch (RuntimeException e) {
                        log.error(e.getMessage());
                        execute(new AnswerCallback(msg.getCallbackId(), "\u2705"));
                    }
                    break;
                case "Назад":
                    execute(new InlineKeyboardMessage(chatId, menuMsg, menuButtons)
                            .setMessageId(msg.getId())
                            .setNumOfColumns(2));
                    currentLvl = menuLvl;
                    break;
                default:
                    if (!msg.getCallbackId().isEmpty()) {
                        execute(new AnswerCallback(msg.getCallbackId(), "Команда не найдена в разделе"));
                        delete(msg.getId());
                    }
            }
        };

        homeControlLvl = msg -> {
            log.info("HomeControl level");
            switch (msg.getText()) {
                case "devices":
                    goToDevices(null, msg);
                    break;
                case "token_gen":
                    execute(new Message(chatId, service.createToken(chatId))
                            .setMessageId(msg.getId()));
                    // Сообщение с токеном
                    execute(new InlineKeyboardMessage(chatId, tokenSuccessGen, null)
                            .hasBackButton(true));
                    break;
                case "Назад":
                    execute(new InlineKeyboardMessage(chatId, menuMsg, menuButtons)
                            .setMessageId(msg.getId())
                            .setNumOfColumns(2));
                    currentLvl = menuLvl;
                    break;
                default:
                    if (!msg.getCallbackId().isEmpty()) {
                        execute(new AnswerCallback(msg.getCallbackId(), "Команда не найдена в разделе"));
                        delete(msg.getId());
                    }
            }
        };

        devicesLvl = msg -> {
            log.info("Devices level");
            if (msg.getText().equals("Добавить")) {
                /*creator = new DeviceCreator();
                messages.add(creator.goToStepOne());*/
            } else if (msg.getText().equals("Назад")) {
                goToHomeControlLevel(msg);
            }
            // Если ответ содержит только число, то пытаемся получить устройство с таким id
            else if (msg.getText().matches("[-+]?\\d+")) {
                try {
                    goToDevice(msg, Integer.parseInt(msg.getText()));
                    currentLvl = deviceControlLvl;
                } catch (ChannelNotFoundException e) {
                    log.error(e.getMessage());
                    goToHomeControlLevel(msg);
                }
            } else {
                execute(new Message(chatId, String.format(unknownCmdSection, msg.getText())));
            }
        };

        deviceControlLvl = msg -> {
            String[] arr = msg.getText().split("[_]");
            String cmd = arr[0];
            int deviceId = 0;
            if (arr.length > 1) {
                deviceId = Integer.parseInt(arr[1]);
            }

            switch (cmd) {
                case "off":
                    try {
                        setDigitalState(deviceId, false);
                        goToDevice(msg, deviceId);
                        execute(new AnswerCallback(msg.getCallbackId(), "Устройство успешно выключено"));
                    } catch (ChannelNotFoundException e) {
                        log.error(e.getMessage());
                        goToHomeControlLevel(msg);
                    }
                    break;
                case "on":
                    try {
                        setDigitalState(deviceId, true);
                        goToDevice(msg, deviceId);
                        execute(new AnswerCallback(msg.getCallbackId(), "Устройство успешно включено"));
                    } catch (ChannelNotFoundException e) {
                        log.error(e.getMessage());
                        goToHomeControlLevel(msg);
                    }
                    break;
                case "remove":
                    try {
                        deleteOutput(Integer.parseInt(arr[1]));
                        goToDevices("Устройство успешно удалено", msg);
                    } catch (ChannelNotFoundException e) {
                        log.error(e.getMessage());
                        goToHomeControlLevel(msg);
                    }
                    break;
                case "Назад":
                    goToDevices(null, msg);
                    break;
                default:
                    execute(new Message(chatId, unknownCmdSection));
            }
        };
    }

    void sendAnswer(IncomingMessage msg) {
        if (!defaultLvl.test(msg)) {
            currentLvl.accept(msg);
        }
        /*swich (level) {
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
                    */
    }

    // LEVELS
    private void goToHomeControlLevel(IncomingMessage msg) {
        if (service.isChannelExist(chatId)) {
            execute(new InlineKeyboardMessage(chatId, homeControl, homeControlButtons)
                    .setNumOfColumns(2)
                    .setMessageId(msg.getId())
                    .hasBackButton(true));
            currentLvl = homeControlLvl;
        } else {
            execute(new AnswerCallback(msg.getCallbackId(), channelNotFound).setAlert(true));
            if (currentLvl != menuLvl) {
                execute(new InlineKeyboardMessage(chatId, menuMsg, menuButtons)
                        .setMessageId(msg.getId())
                        .setNumOfColumns(2));
                currentLvl = menuLvl;
            }
        }
    }

    private void goToDevices(String text, IncomingMessage msg) {
        try {
            List<CallbackButton> devices = new ArrayList<>();

            for (Output output : getOutputs()) {
                devices.add(new CallbackButton(output.getName(), String.valueOf(output.getOutputId())));
            }
            String answerText;
            if (devices.isEmpty()) {
                answerText = "Устройства не обнаружены. Вы можете добавить их прямо сейчас";
            } else answerText = devicesMsg;

            if (text != null) execute(new AnswerCallback(msg.getCallbackId(), text));

            execute(new InlineKeyboardMessage(chatId, answerText, devices)
                    .hasAddButton(true)
                    .hasBackButton(true)
                    .setMessageId(msg.getId())
                    .setNumOfColumns(1));
            currentLvl = devicesLvl;
        } catch (ChannelNotFoundException e) {
            log.warn(e.getMessage());
            goToHomeControlLevel(msg);
        }
    }

    private void goToDevice(IncomingMessage msg, int deviceId) throws ChannelNotFoundException {
        Output output = getOutput(deviceId);

        if (output == null) goToDevices("Устройство c таким id не найдено", msg);
        else {
            boolean currState = getDigitalState(output.getOutputId());
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
                if(currState) add(new CallbackButton("Выключить", "off_" + output.getOutputId()));
                else add(new CallbackButton("Включить", "on_" + output.getOutputId()));
                add(new CallbackButton("Удалить", "remove_" + output.getOutputId()));
            }};

            execute(new InlineKeyboardMessage(chatId, String.format("<b>%s</b>\n" +
                            "Текущее состояние: <i>%s</i>\n" +
                            "Тип сигнала: <i>%s</i>\n" +
                            "Инверсия: <i>%s</i>\n" +
                            "GPIO-пин: <i>%d</i>",
                    output.getName(), currStateText, signalType, inversion,
                    output.getGpio()), buttons)
                    .setNumOfColumns(2)
                    .setMessageId(msg.getId())
                    .hasBackButton(true));
        }
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
        log.info("Id: " + outputId + " signal: " + state);
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
        return service.getChannel(chatId);
    }

    // **************************************************************************************************************

    /* **************************************************************************************************************
     ************************************** СОЗДАНИЕ УСТРОЙСТВА *****************************************************
     ************************************************************************************************************** */

    /*private class DeviceCreator {

        @Getter(value = AccessLevel.PRIVATE)
        private Output creationOutput;

        @Getter(value = AccessLevel.PRIVATE)
        @Setter(value = AccessLevel.PRIVATE)
        private int step = 0;

        private DeviceCreator() {
            creationOutput = new Output();
        }

        private Message goToPrev() {
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

    private Message goToStepOne() {
        Message msg = new Message(chatId, "Пожалуйста, введите имя нового устройства")
                .hasBackButton(true);
        level = 4;
        subLevel = 1;
        step = 1;
        return msg;
    }

    // Step one - SET UP DEVICE NAME
    private Message setOutputName(String name) {
        creationOutput.setName(name);
        return goToStepTwo();
    }

    private Message goToStepTwo() {
        Message msg = new Message(chatId, "Отлично! Теперь наберите тип сигнала, который " +
                "может принимать устройство", typesOfSignal)
                .setNumOfColumns(2)
                .hasBackButton(true);
        step = 2;
        return msg;
    }

    private Message setOutputSignalType(String signalType) {
        Message msg;
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
                msg = new Message(chatId, "Вы ввели несуществующий тип сигнала", typesOfSignal)
                        .setNumOfColumns(2)
                        .hasBackButton(true);
        }
        return msg;
    }

    private Message goToStepThree(String signalType) {
        Message msg = null;
        try {
            switch (signalType) {
                case "digital":
                    msg = new Message(chatId, "Теперь выберите пин, к которому вы хотите подключить новое " +
                            "устройство", getAvailableOutputs("digital"))
                            .setNumOfColumns(4)
                            .hasBackButton(true);
                    step = 3;
                    break;
                case "pwm":
                    msg = new Message(chatId, "Теперь выберите пин, к которому вы хотите подключить новое " +
                            "устройство", getAvailableOutputs("pwm"))
                            .setNumOfColumns(4)
                            .hasBackButton(true);
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

    private Message setOutputGpio(String gpioStr) {
        Message msg;
        try {
            if (contains(gpioStr, getAvailableOutputs(creationOutput.getType()))) {
                creationOutput.setGpio(Integer.valueOf(gpioStr));
                msg = goToStepFour();
            } else
                msg = new Message(chatId, "Выберите предложенный в списке выход",
                        getAvailableOutputs(creationOutput.getType()))
                        .hasBackButton(true)
                        .setNumOfColumns(4);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, e.getMessage());
            try {
                msg = new Message(chatId, "Вы ввели не число", getAvailableOutputs(creationOutput.getType()))
                        .hasBackButton(true)
                        .setNumOfColumns(4);
            } catch (ChannelNotFoundException ex) {
                LOGGER.log(Level.WARNING, e.getMessage());
                msg = goToHomeControlLevel();
            }
        } catch (ChannelNotFoundException e) {
            LOGGER.log(Level.WARNING, e.getMessage());
            msg = goToHomeControlLevel();
        }
        return msg;
    }

    private Message goToStepFour() {
        Message msg = new Message(chatId, "Сделать инверсию сигнала для данного устройства?", yesOrNo)
                .setNumOfColumns(2)
                .hasBackButton(true);
        step = 4;
        return msg;
    }

    private Message setOutputReverse(String reverse) {
        switch (reverse) {
            case "да":
                creationOutput.setReverse(true);
                break;
            case "нет":
                creationOutput.setReverse(false);
                break;
            default:
                return new Message(chatId, "Ответ должен быть либо \"Да\", либо \"Нет\"", yesOrNo)
                        .hasBackButton(true)
                        .setNumOfColumns(2);
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
}*/
    private void delete(int messageId) {
        log.info("Removing message...");
        DeleteMessage message = new DeleteMessage(chatId, messageId);
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void execute(AnswerCallback callback) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery()
                .setCallbackQueryId(callback.getCallbackId())
                .setText(callback.getText())
                .setShowAlert(callback.isAlert());
        try {
            bot.execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void execute(Message msg) {
        log.info("Sending message...");
        if (msg.getMessageId() == 0) {
            SendMessage answer = new SendMessage(msg.getChatId(), msg.getText())
                    .setParseMode("HTML");
            try {
                bot.execute(answer);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            EditMessageText answer = new EditMessageText()
                    .setChatId(msg.getChatId())
                    .setText(msg.getText())
                    .setMessageId(msg.getMessageId())
                    .setParseMode("HTML");
            try {
                bot.execute(answer);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void execute(InlineKeyboardMessage msg) throws RuntimeException {
        log.info("Sending message...");
        InlineKeyboardBuilder builder = InlineKeyboardBuilder.create(chatId);

        if (msg.getButtons() != null) {
            Iterator<CallbackButton> buttons = msg.getButtons().iterator();
            while (buttons.hasNext()) {
                for (int i = 0; i < msg.getNumOfColumns(); i++) {
                    if (buttons.hasNext()) {
                        CallbackButton button = buttons.next();
                        builder.button(button.getText(), button.getCallbackText());
                    } else break;
                }
                builder.endRow().row();
            }
            builder.endRow();
        }

        if (msg.isRemoveButton())
            builder.row().button("Удалить").endRow();

        builder.row();
        if (msg.isBackButton())
            builder.button("Назад");
        if (msg.isAddButton())
            builder.button("Добавить");
        builder.endRow();

        builder.setText(msg.getText());

        if (msg.getMessageId() == 0) {
            SendMessage answer = builder.buildNew();
            try {
                bot.execute(answer);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            EditMessageText answer = builder.setMessageId(msg.getMessageId()).buildEdited();
            try {
                bot.execute(answer);
            } catch (TelegramApiRequestException e) {
                throw new RuntimeException();
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
}