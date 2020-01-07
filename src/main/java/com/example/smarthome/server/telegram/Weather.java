package com.example.smarthome.server.telegram;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

class Weather {
    private static final String URL =
            "https://api.openweathermap.org/data/2.5/weather?appid=3156e4747f7d07492a0c3a19b388ed8f&id=550280&lang=ru" +
                    "&units=metric";
    private static final DecimalFormat df;
    private static final HttpClient client;
    public static final Logger log;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');

        client = HttpClientBuilder.create().build();

        df = new DecimalFormat();
        df.setDecimalFormatSymbols(symbols);
        df.setGroupingSize(3);
        df.setMaximumFractionDigits(2);

        log = LoggerFactory.getLogger(Weather.class);
    }

    String getWeather() {
        log.info("Build request..");
        HttpUriRequest request = new HttpGet(URL);

        String weather = null;

        try {
            log.info("Send..");
            final HttpEntity entity = client.execute(request).getEntity();
            log.info("Parse data...");
            StringBuilder message = new StringBuilder();
            JSONObject obj = new JSONObject(EntityUtils.toString(entity));

            message.append("<b>Текущая погода в Химках</b>\n");

            // Температура
            JSONObject main = obj.getJSONObject("main");

            message.append(String.format(Locale.ENGLISH, "• Температура %.2f °C\n",
                    main.getFloat("temp")));
            message.append(String.format(Locale.ENGLISH, "• Ощущается как %.2f °C\n",
                    main.getFloat("feels_like")));

            // Влажность
            message.append(String.format("• Влажность %d %%\n",
                    main.getInt("humidity")));

            // Описание
            StringBuilder description = new StringBuilder();
            JSONArray array = obj.getJSONArray("weather");

            for (int i = 0; i < array.length(); i++) {
                JSONObject tmp = array.getJSONObject(i);
                description.append(tmp.getString("description"));

                if (i + 1 < array.length())
                    description.append(", ");
            }

            message.append(String.format("• На улице %s\n", description.toString()));

            // ВЕТЕР
            JSONObject wind = obj.getJSONObject("wind");
            message.append(String.format("• Скорость ветра %d м/с\n",
                    wind.getInt("speed")));
            message.append(String.format("• Направление ветра %s\n",
                    getWindDir((wind.getInt("deg")))));

            // ОБЛАЧНОСТЬ
            JSONObject clouds = obj.getJSONObject("clouds");
            message.append(String.format("• Облачность %d %%\n", clouds.getInt("all")));

            // Давление
            int hPa = main.getInt("pressure");
            message.append(String.format(Locale.ENGLISH, "• Давление %.2f мм рт.ст.\n",
                    hPa * 0.750063 -  17.2));

            // Видимость
            message.append(String.format("• Видимость %s м\n", df.format(obj.getInt("visibility"))));

            weather = message.toString();
            log.info("Parsing finish!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return weather;
    }

    private String getWindDir(int degrees) {
        if (degrees >= 337) {
            return "северное";
        } else if (degrees >= 292) {
            return "северо-западное";
        } else if (degrees >= 247) {
            return "западное";
        } else if (degrees >= 202) {
            return "юго-западное";
        } else if (degrees >= 157) {
            return "южное";
        } else if (degrees >= 112) {
            return "юго-восточное";
        } else if (degrees >= 67) {
            return "восточное";
        }
        return "северо-восточное";
    }
}
