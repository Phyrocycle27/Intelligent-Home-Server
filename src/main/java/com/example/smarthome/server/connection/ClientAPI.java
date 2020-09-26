package com.example.smarthome.server.connection;

import com.example.smarthome.server.entity.Device;
import com.example.smarthome.server.entity.signal.SignalType;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.exceptions.OutputAlreadyExistException;
import com.example.smarthome.server.exceptions.OutputNotFoundException;
import com.example.smarthome.server.service.DeviceAccessService;
import com.google.gson.*;
import io.netty.channel.Channel;
import lombok.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ClientAPI {

    private static final DeviceAccessService service = DeviceAccessService.getInstance();
    private static final Logger log = LoggerFactory.getLogger(ClientAPI.class);
    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>)
                    (json, type, jsonDeserializationContext) ->
                            LocalDateTime.parse(json.getAsJsonPrimitive().getAsString(),
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>)
                    (localDateTime, type, jsonSerializationContext) ->
                            new JsonPrimitive(localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
            .create();

    public static boolean getDigitalState(Channel ch, @NonNull Integer id) throws ChannelNotFoundException,
            OutputNotFoundException {
        JSONObject request = buildRequest(new JSONObject()
                .put("method", "GET")
                .put("uri", "http://localhost:8080/devices/control/digital?id=" + id));

        JSONObject response = JsonRequester.execute(request, ch);

        if (response.getInt("code") == 200) {
            return response.getJSONObject("entity").getBoolean("digital_state");
        } else {
            throw new OutputNotFoundException(id);
        }
    }

    public static void setDigitalState(Channel ch, @NonNull Integer deviceId, boolean state)
            throws ChannelNotFoundException, OutputNotFoundException {

        JSONObject request = buildRequest(new JSONObject()
                .put("method", "PUT")
                .put("uri", "http://localhost:8080/devices/control/digital")
                .put("request_body", new JSONObject()
                        .put("id", deviceId)
                        .put("digital_state", state)));

        JSONObject response = JsonRequester.execute(request, ch);

        if (response.getInt("code") != 200) {
            throw new OutputNotFoundException(deviceId);
        }
    }

    public static int getPwmSignal(Channel ch, @NonNull Integer id) throws ChannelNotFoundException,
            OutputNotFoundException {
        JSONObject request = buildRequest(new JSONObject()
                .put("method", "GET")
                .put("uri", "http://localhost:8080/devices/control/pwm?id=" + id));

        JSONObject response = JsonRequester.execute(request, ch);

        if (response.getInt("code") == 200) {
            return response.getJSONObject("entity").getInt("pwm_signal");
        } else {
            throw new OutputNotFoundException(id);
        }
    }

    public static void setPwmSignal(Channel ch, @NonNull Integer deviceId, int signal)
            throws ChannelNotFoundException, OutputNotFoundException {

        JSONObject request = buildRequest(new JSONObject()
                .put("method", "PUT")
                .put("uri", "http://localhost:8080/devices/outputs/control/pwm")
                .put("request_body", new JSONObject()
                        .put("id", deviceId)
                        .put("pwm_signal", signal)));

        JSONObject response = JsonRequester.execute(request, ch);

        if (response.getInt("code") != 200) {
            throw new OutputNotFoundException(deviceId);
        }
    }

    // DELETE
    public static void deleteDevice(Channel ch, Integer deviceId) throws ChannelNotFoundException, OutputNotFoundException {
        JSONObject request = buildRequest(new JSONObject()
                .put("method", "DELETE")
                .put("uri", "http://localhost:8080/devices/one/" + deviceId));

        JSONObject response = JsonRequester.execute(request, ch);

        if (response.getInt("code") != 200) {
            throw new OutputNotFoundException(deviceId);
        }
    }

    // CREATE
    public static void createDevice(Channel ch, Device newDevice) throws ChannelNotFoundException,
            OutputAlreadyExistException {
        JSONObject request = buildRequest(new JSONObject()
                .put("method", "POST")
                .put("uri", "http://localhost:8080/devices/create")
                .put("request_body", new JSONObject(newDevice)));

        JSONObject response = JsonRequester.execute(request, ch);

        if (response.getInt("code") == 409) {
            throw new OutputAlreadyExistException(newDevice.getGpio().getGpio());
        }
    }

    public static void updateDevice(Channel ch, Device updatedDevice) throws ChannelNotFoundException,
            OutputNotFoundException {
        JSONObject request = buildRequest(new JSONObject()
                .put("method", "PUT")
                .put("uri", "http://localhost:8080/devices/one/" + updatedDevice.getId())
                .put("request_body", new JSONObject(gson.toJson(updatedDevice))));

        JSONObject response = JsonRequester.execute(request, ch);

        if (response.getInt("code") != 200) {
            throw new OutputNotFoundException(updatedDevice.getId());
        }
    }

    // GET
    public static Device getDevice(Channel ch, Integer deviceId) throws ChannelNotFoundException,
            OutputNotFoundException {
        // ***********************************************************
        JSONObject request = buildRequest(new JSONObject()
                .put("method", "GET")
                .put("uri", "http://localhost:8080/devices/one/" + deviceId));
        // ***********************************************************

        JSONObject response = JsonRequester.execute(request, ch);

        if (response.getInt("code") == 200) {
            return gson.fromJson(response.getJSONObject("entity").toString(), Device.class);
        } else {
            throw new OutputNotFoundException(deviceId);
        }
    }

    // GET LIST
    public static List<Device> getDevices(Channel ch) throws ChannelNotFoundException {
        List<Device> devices = new ArrayList<>();

        // ***********************************************************
        JSONObject request = buildRequest(new JSONObject()
                .put("method", "GET")
                .put("uri", "http://localhost:8080/devices/all"));
        // ***********************************************************
        JSONArray response = JsonRequester.execute(request, ch).getJSONArray("entity");

        if (response.length() != 0) {
            // Создаём из объектов массива JSON объекты Device
            // и вносим их в List outputs
            for (int i = 0; i < response.length(); i++) {
                JSONObject outputJson = response.getJSONObject(i);
                Device device = new Device();

                device.setName(outputJson.getString("name"));
                device.setId(outputJson.getInt("id"));

                devices.add(device);
            }
        }
        return devices;
    }

    // GET LIST CONTAINS NUMBERS OF FREE GPIOS
    public static List<String> getAvailableGPIOS(Channel ch, SignalType type) throws ChannelNotFoundException {
        List<String> gpios = new ArrayList<>();

        JSONObject request = buildRequest(new JSONObject()
                .put("method", "GET")
                .put("uri", "http://localhost:8080/util/gpio/available?type=" + type.name().toLowerCase()));

        JSONArray array = JsonRequester.execute(request, ch)
                .getJSONObject("entity")
                .getJSONArray("available_gpio_pins");

        for (int i = 0; i < array.length(); i++) {
            gpios.add(String.valueOf(array.getInt(i)));
        }

        return gpios;
    }

    public static JSONObject buildRequest(JSONObject body) {
        return new JSONObject()
                .put("type", "request")
                .put("body", body);
    }

    public static Channel getChannel(long chatId) throws ChannelNotFoundException {
        return service.getChannel(chatId);
    }
}
