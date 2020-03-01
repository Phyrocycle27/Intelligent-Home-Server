package com.example.smarthome.server.connection;

import com.example.smarthome.server.entity.Output;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
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

    public static boolean getDigitalState(Channel ch, @NonNull Integer outputId) throws ChannelNotFoundException {
        JSONObject request = buildRequest(new JSONObject()
                .put("method", "GET")
                .put("uri", "http://localhost:8080/outputs/control/digital?id=" + outputId));

        return JsonRequester.execute(request, ch)
                .getJSONObject("entity")
                .getBoolean("digitalState");
    }

    public static void setDigitalState(Channel ch, @NonNull Integer outputId, boolean state)
            throws ChannelNotFoundException {

        JSONObject request = buildRequest(new JSONObject()
                .put("method", "PUT")
                .put("uri", "http://localhost:8080/outputs/control/digital")
                .put("request_body", new JSONObject()
                        .put("outputId", outputId)
                        .put("digitalState", state)));

        JsonRequester.execute(request, ch);
    }

    // DELETE
    public static void deleteOutput(Channel ch, Integer outputId) throws ChannelNotFoundException {
        JSONObject request = buildRequest(new JSONObject()
                .put("method", "DELETE")
                .put("uri", "http://localhost:8080/outputs/one/" + outputId));

        JsonRequester.execute(request, ch);
    }

    // CREATE
    public static void createOutput(Channel ch, Output newOutput) throws ChannelNotFoundException {
        JSONObject request = buildRequest(new JSONObject()
                .put("method", "POST")
                .put("uri", "http://localhost:8080/outputs/create")
                .put("request_body", new JSONObject(newOutput)));

        JsonRequester.execute(request, ch);
    }

    public static void updateOutput(Channel ch, Output updatedOutput) throws ChannelNotFoundException {
        JSONObject request = buildRequest(new JSONObject()
                .put("method", "PUT")
                .put("uri", "http://localhost:8080/outputs/one/" + updatedOutput.getOutputId())
                .put("request_body", new JSONObject(gson.toJson(updatedOutput))));

        JsonRequester.execute(request, ch);
    }

    // GET
    public static Output getOutput(Channel ch, Integer outputId) throws ChannelNotFoundException {
        Output output = new Output();

        // ***********************************************************
        JSONObject request = buildRequest(new JSONObject()
                .put("method", "GET")
                .put("uri", "http://localhost:8080/outputs/one/" + outputId));
        // ***********************************************************

        JSONObject response = JsonRequester.execute(request, ch);

        if (response.getInt("code") == 200) {
            return gson.fromJson(response.getJSONObject("entity").toString(), Output.class);
        } else {
            return null;
        }
    }

    // GET LIST
    public static List<Output> getOutputs(Channel ch) throws ChannelNotFoundException {
        List<Output> outputs = new ArrayList<>();

        // ***********************************************************
        JSONObject request = buildRequest(new JSONObject()
                .put("method", "GET")
                .put("uri", "http://localhost:8080/outputs/all"));
        // ***********************************************************
        JSONArray response = JsonRequester.execute(request, ch).getJSONArray("entity");

        if (response.length() != 0) {
            // Создаём из объектов массива JSON объекты Output
            // и вносим их в List outputs
            for (int i = 0; i < response.length(); i++) {
                JSONObject outputJson = response.getJSONObject(i);
                Output output = new Output();

                output.setName(outputJson.getString("name"));
                output.setOutputId(outputJson.getInt("outputId"));

                outputs.add(output);
            }
        }
        return outputs;
    }

    // GET LIST CONTAINS NUMBERS OF FREE GPIOS
    public static List<String> getAvailableOutputs(Channel ch, String type) throws ChannelNotFoundException {
        List<String> gpios = new ArrayList<>();

        JSONObject request = buildRequest(new JSONObject()
                .put("method", "GET")
                .put("uri", "http://localhost:8080/outputs/available?type=" + type));

        JSONArray array = JsonRequester.execute(request, ch)
                .getJSONObject("entity")
                .getJSONArray("available_gpios");

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
