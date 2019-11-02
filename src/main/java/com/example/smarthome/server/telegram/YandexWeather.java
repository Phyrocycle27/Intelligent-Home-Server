package com.example.smarthome.server.telegram;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;

class YandexWeather {

    private static final String URL =
            "https://api.weather.yandex.ru/v1/forecast?lat=55.75396&lon=37.620393&limit=2&hours=false&extra=true";
    private static final String API_KEY = "0fc17113-0ba8-41f1-aacd-e4ed400b186f";

    String getWeather() {

        HttpClient client = HttpClientBuilder.create().build();

        HttpUriRequest request = new HttpGet(URL);
        request.setHeader("X-Yandex-API-Key", API_KEY);

        String weather = null;

        try {
            final HttpEntity entity = client.execute(request).getEntity();

            StringBuilder message = new StringBuilder();
            JSONObject obj = new JSONObject(EntityUtils.toString(entity));
            JSONObject fact = obj.getJSONObject("fact");

            message.append("Текущая погода в Химках\n");
            message.append(String.format("• Температура %d°C\n",
                    fact.getInt("temp")));

            message.append(String.format("  Ощущается как %d°C\n",
                    fact.getInt("feels_like")));

            message.append(String.format("• Влажность %d%s\n",
                    fact.getInt("humidity"), "%"));

            message.append(String.format("• Давление %d мм рт.ст.\n",
                    fact.getInt("pressure_mm")));
            message.append(String.format("• Скорость ветра %d м/с\n",
                    fact.getInt("wind_speed")));

            message.append(String.format("  Скорость порывов ветра %d м/с\n",
                    fact.getInt("wind_gust")));

            message.append(String.format("• Направление ветра %s\n",
                    getWindDir(fact.getString("wind_dir"))));

            message.append(String.format("• На улице %s",
                    getPrecWithStrength(fact.getInt("prec_type"),
                            fact.getDouble("prec_strength"))));

            weather = message.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return weather;
    }

    private String getWindDir(String s) {
        switch (s) {
            case "nw":
                return "северо-западное";
            case "n":
                return "северное";
            case "ne":
                return "сверо-восточное";
            case "e":
                return "восточное";
            case "se":
                return "юго-восточное";
            case "s":
                return "южное";
            case "sw":
                return "юго-западное";
            case "w":
                return "западное";
            case "с":
                return "штиль";
            default:
                return "неизвестно";
        }
    }

    private String getPrecWithStrength(int precCode, double strength) {
        String prec = getPrec(precCode);
        if (strength == 1) return prec.equals("дождь") ? "сильный ливень" : "очень сильный" + prec;
        else if (strength == 0.75) return "сильный" + prec;
        else if (strength == 0.5) return prec;
        else if (strength == 0.25) return "слабый" + prec;
        else return prec;
    }

    private String getPrec(int precCode) {
        switch (precCode) {
            case 0:
                return "без осадков";
            case 1:
                return "дождь";
            case 2:
                return "дождь со снегом";
            case 3:
                return "снег";
            default:
                return "...впрочем, это не важно";
        }
    }

}
