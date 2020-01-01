package com.example.smarthome.server.telegram;

import com.example.smarthome.server.connection.JsonRequester;
import com.example.smarthome.server.entity.Output;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.service.DeviceAccessService;
import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
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
    private static final String sensorsMsg = "Нажмите на существующий датчик или добавьте новый";
    private static final String devicesMsg = "Нажмите на существующее устройство или добавьте новое";
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
    private static Bot bot;

    /* ************************************ TEMP VARIABLES ************************************************** */
    private Map<String, Integer> outputsMap;
//    private DeviceCreator creator;

    private int level = 0;
    private int subLevel = 0;
    private int outputId;
    private long chatId;

    private Consumer<String> currentLvl;
    private Consumer<String> defaultLvl;
    private Consumer<String> menuLvl;
    private Consumer<String> infoLvl;
    private Consumer<String> homeControlLvl;
    private Consumer<String> devicesLvl;

    static {
        LOGGER = Logger.getLogger(Bot.class.getName());
        weatherService = new Weather();
        service = DeviceAccessService.getInstance();
    }

    UserInstance(long chatId) {
        this.chatId = chatId;
        init();
        currentLvl = defaultLvl;
    }

    public static void setBot(Bot bot) {
        UserInstance.bot = bot;
    }

    private void init() {
        defaultLvl = s -> {
            if (s.toLowerCase().equals("меню") || s.equals("/start")) {
                execute(new ReplyKeyboardMessage(chatId, menuMsg, menuButtons)
                        .setNumOfColumns(2));
                currentLvl = menuLvl;
            } else
                execute(new Message(chatId, String.format(unknownCmdMain, s)));

        };

        menuLvl = s -> {
            switch (s) {
                case "Управление домом":
                    if (service.isExists(chatId))
                        goToHomeControlLevel();
                    else {
                        execute(new ReplyKeyboardMessage(chatId, tokenNotFound, tokenGenBtn)
                                .hasBackButton(true));
                    }
                    break;
                case "Информация":
                    execute(new ReplyKeyboardMessage(chatId, infoMsg, infoButtons)
                            .setNumOfColumns(2)
                            .hasBackButton(true));
                    currentLvl = infoLvl;
                    break;
                default:
                    execute(new Message(chatId, String.format(defaultSection, s)));
            }
        };

        infoLvl = s -> {
            switch (s) {
                case "Погода":
                    String weather = weatherService.getWeather();
                    String answer;

                    if (weather == null) answer = "Ошибка при получении погоды";
                    else answer = weather;

                    execute(new Message(chatId, answer));
                    break;
                case "Время":
                    execute(new Message(chatId, String.format("Химкинское время %s",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))));
                    break;
                case "Назад":
                    execute(new ReplyKeyboardMessage(chatId, menuMsg, menuButtons)
                            .setNumOfColumns(2));
                    currentLvl = menuLvl;
                    break;
                default:
                    execute(new Message(chatId, String.format(defaultSection, s)));
            }
        };

        homeControlLvl = s -> {
            switch (s) {
                case "Устройства":
                    goToDevices(null);
                    break;
                case "Датчики":
                    List<String> inputsBtns = new ArrayList<>();
                    execute(new ReplyKeyboardMessage(chatId, sensorsMsg, inputsBtns)
                            .hasBackButton(true)
                            .hasAddButton(true));
                    break;
                case "Сгенерировать токен":
                    execute(new Message(chatId, tokenSuccessGen));
                    // Сообщение с токеном
                    execute(new ReplyKeyboardMessage(chatId, service.createToken(chatId), new String[]{})
                            .hasBackButton(true));
                    break;
                case "Назад":
                    execute(new ReplyKeyboardMessage(chatId, menuMsg, menuButtons)
                            .setNumOfColumns(2));
                    currentLvl = menuLvl;
                default:
                    execute(new Message(chatId, String.format(defaultSection, s)));
            }
        };

        devicesLvl = s -> {
            if (s.equals("Добавить")) {
                /*creator = new DeviceCreator();
                messages.add(creator.goToStepOne());*/
            } else if (outputsMap.containsValue(Integer.parseInt(s))) {
                try {
                    Output output = getOutput(Integer.valueOf(s));

                    if (output == null) goToDevices("Устройство не найдено");
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
                        new ReplyKeyboardMessage(chatId, String.format("<b>%s</b>\n" +
                                        "Текущее состояние: <i>%s</i>\n" +
                                        "Тип сигнала: <i>%s</i>\n" +
                                        "Инверсия: <i>%s</i>\n" +
                                        "GPIO-пин: <i>%d</i>",
                                output.getName(), currState, signalType, inversion,
                                output.getGpio()), onOrOff)
                                .setNumOfColumns(2)
                                .hasBackButton(true)
                                .hasRemoveButton(true);
                        outputId = output.getOutputId();

                    }
                } catch (ChannelNotFoundException e) {
                    goToHomeControlLevel();
                    e.printStackTrace();
                }
            } else {
                execute(new Message(chatId, String.format(defaultSection, s)));
            }
        };
    }

    void sendAnswer(String incoming) {
        // если пользователь нам прислал команду "меню" или "/start", то отправляем ему главное меню
        currentLvl.accept(incoming);
        // а вот тут-то мы и прорабатываем весь сценарий бота и его команды
        /*switch (level) {
            /* ***********************************
             __________________   ____________
            | Управление домом | | Информация |
             ¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯   ¯¯¯¯¯¯¯¯¯¯¯¯
            *********************************** */
            /*case 1:
                switch (incoming) {
                    case "Управление домом":
                        if (service.isExists(chatId)) {
                            messages.add(goToHomeControlLevel());
                        } else {
                            messages.add(new Message(chatId, tokenNotFound, tokenGenBtn)
                                    .hasBackButton(true));
                            subLevel = 1;
                            level = 2;
                        }
                        break;
                    case "Информация":
                        messages.add(new Message(chatId, infoMsg, infoButtons)
                                .setNumOfColumns(2)
                                .hasBackButton(true));
                        subLevel = 2;
                        level = 2;
                        break;
                    default:
                        messages.add(new Message(chatId, String.format(defaultSection, incoming)));
                }
                break;
            case 2:
                if (incoming.equals("Назад")) {
                    messages.add(new Message(chatId, menuMsg, menuButtons)
                            .setNumOfColumns(2));
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
                    /*case 1:
                        switch (incoming) {
                            case "Устройства":
                                messages.add(goToDevices(null));
                                break;
                            case "Датчики":
                                // Запрашиваем с raspberry pi все подключенные датчики и выводим их как кнопки
                                List<String> inputsBtns = new ArrayList<>();
                                messages.add(new Message(chatId, sensorsMsg, inputsBtns)
                                        .hasBackButton(true)
                                        .hasAddButton(true));
                                level = 3;
                                subLevel = 2;
                                break;
                            case "Сгенерировать токен":
                                messages.add(new Message(chatId, tokenSuccessGen));
                                // Сообщение с токеном
                                messages.add(new Message(chatId, service.createToken(chatId))
                                        .hasBackButton(true));
                                break;
                            default:
                                messages.add(new Message(chatId, String.format(defaultSection, incoming)));
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
                    /*case 2:
                        switch (incoming) {
                            case "Погода":
                                String weather = weatherService.getWeather();
                                String answer;

                                if (weather == null) answer = "Ошибка при получении погоды";
                                else answer = weather;

                                messages.add(new Message(chatId, answer));
                                break;
                            case "Время":
                                messages.add(new Message(chatId, String.format("Химкинское время %s",
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))));
                                break;
                            default:
                                messages.add(new Message(chatId, String.format(defaultSection, incoming)));
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
                // Устройства
                if (subLevel == 1) {
                    if (incoming.equals("Добавить")) {
                        creator = new DeviceCreator();
                        messages.add(creator.goToStepOne());
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
                                new Message(chatId, String.format("<b>%s</b>\n" +
                                                "Текущее состояние: <i>%s</i>\n" +
                                                "Тип сигнала: <i>%s</i>\n" +
                                                "Инверсия: <i>%s</i>\n" +
                                                "GPIO-пин: <i>%d</i>",
                                        output.getName(), currState, signalType, inversion,
                                        output.getGpio()), onOrOff)
                                        .setNumOfColumns(2)
                                        .hasBackButton(true)
                                        .hasRemoveButton(true);
                                level = 4;
                                subLevel = 2;
                                outputId = output.getOutputId();
                            }

                        } catch (ChannelNotFoundException e) {
                            goToHomeControlLevel();
                            e.printStackTrace();
                        }
                    } else {
                        messages.add(new Message(chatId, String.format(defaultSection, incoming)));
                    }
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
                                messages.add(new Message(chatId, "Устройство успешно включено"));
                                setDigitalState(outputId, true);
                            } catch (ChannelNotFoundException e) {
                                e.printStackTrace();
                            }
                        } else if (incoming.equals("Выключить")) {
                            try {
                                messages.add(new Message(chatId, "Устройство успешно выключено"));
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
                                    messages.add(new Message(chatId, defaultSection));
                            }
                        }
                }
                break;
        }*/
    }

    // LEVELS
    private void goToHomeControlLevel() {
        if (service.isChannelExist(chatId)) {
            execute(new ReplyKeyboardMessage(chatId, homeControl, homeControlBtns)
                    .setNumOfColumns(2)
                    .hasBackButton(true));
            currentLvl = homeControlLvl;
        } else {
            execute(new ReplyKeyboardMessage(chatId, channelNotFound, menuButtons)
                    .setNumOfColumns(2));
            currentLvl = menuLvl;
        }
    }

    private void goToDevices(String text) {
        try {
            if (outputsMap == null) outputsMap = new HashMap<>();

            for (Output output : getOutputs()) {
                if (!outputsMap.containsKey(output.getName()))
                    outputsMap.put(output.getName(), output.getOutputId());
            }
            // Таблица <имя_устройства> - <id>
            Map<String, String> map = new HashMap<>();
            for (Map.Entry<String, Integer> entry : outputsMap.entrySet())
                map.put(entry.getKey(), String.valueOf(entry.getValue()));

            String answerText;
            if (text != null)
                answerText = text;
            else answerText = devicesMsg;

            execute(new InlineKeyboardMessage(chatId, answerText, map)
                    .setNumOfColumns(3));
            currentLvl = devicesLvl;
        } catch (ChannelNotFoundException e) {
            LOGGER.log(Level.WARNING, e.getMessage());
            goToHomeControlLevel();
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
        }*/

    /*private Message goToStepOne() {
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
    private void execute(Message msg) {
        LOGGER.log(Level.INFO, msg.getText());
        SendMessage answer = new SendMessage(msg.getChatId(), msg.getText());
        try {
            bot.execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void execute(ReplyKeyboardMessage msg) {
        LOGGER.log(Level.INFO, msg.getText());
        ReplyKeyboardBuilder builder = ReplyKeyboardBuilder.create(chatId);

        List<String> buttons = msg.getButtons();
        for (int i = 1; i <= buttons.size(); i++) {
            builder.button(buttons.get(i - 1));
            if (i % msg.getNumOfColumns() == 0) {
                builder.endRow();
                builder.row();
            }
        }
        builder.endRow();

        if (msg.isRemoveButton())
            builder.row().button("Удалить").endRow();

        builder.row();
        if (msg.isBackButton())
            builder.button("Назад");
        if (msg.isAddButton())
            builder.button("Добавить");
        builder.endRow();

        SendMessage answer = builder.setText(msg.getText()).build();
        try {
            bot.execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    // **************************************************************************************************************
}