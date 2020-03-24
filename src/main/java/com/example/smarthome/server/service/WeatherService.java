package com.example.smarthome.server.service;

import com.example.smarthome.server.entity.City;
import com.example.smarthome.server.entity.WeatherUser;
import com.example.smarthome.server.exceptions.UserNotFoundException;
import com.example.smarthome.server.repository.CitiesRepository;
import com.example.smarthome.server.repository.WeatherUsersRepository;
import lombok.Getter;
import lombok.Setter;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class WeatherService {

    private final String API_KEY = "appid=3156e4747f7d07492a0c3a19b388ed8f";
    private final String URL = "https://api.openweathermap.org/data/2.5/weather?";
    private final String langParam = "&lang=ru";
    private final String unitParam = "&units=metric";
    private final String URI = URL + API_KEY + langParam + unitParam;

    @Getter
    private static final WeatherService instance = new WeatherService();

    @Setter
    private CitiesRepository citiesRepo;
    @Setter
    private WeatherUsersRepository usersRepo;

    private final DecimalFormat df;
    private final HttpClient client;
    private LocationManager locationManager;
    private final Logger log;


    private WeatherService() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');

        client = HttpClientBuilder.create().build();

        df = new DecimalFormat();
        df.setDecimalFormatSymbols(symbols);
        df.setGroupingSize(3);
        df.setMaximumFractionDigits(2);

        locationManager = new LocationManager();

        log = LoggerFactory.getLogger(WeatherService.class);
    }

    public List<City> getUserCities(long userId) throws UserNotFoundException {
        return usersRepo.findById(userId).orElseThrow(() -> new UserNotFoundException(userId)).getCities();
    }

    public void removeCityForUser(long userId, int cityId) {
        WeatherUser user = usersRepo.getOne(userId);
        user.getCities().remove(citiesRepo.getOne(cityId));
        usersRepo.save(user);
    }

    public void addCity(long userId, float lat, float lon) throws Exception {
        //  Получаем пользователя сервиса погоды
        WeatherUser user;
        if (usersRepo.existsById(userId)) {
            user = usersRepo.getOne(userId);
        } else {
            user = new WeatherUser();
            user.setId(userId);
        }

        // Получаем город
        City city;

        int cityId = getCityId(lat, lon);
        log.info("cityId is " + cityId);

        if (cityId == 0) {
            throw new Exception("City's ID is zero");
        }

        if (citiesRepo.existsById(cityId)) {
            city = citiesRepo.getOne(cityId);
        } else {
            city = locationManager.getCity(lat, lon);
            if (city == null) {
                throw new Exception("City is null");
            }
            city.setId(cityId);
        }
        log.info("City is: " + city.toString());

        // Сохраняем изменения в БД
        Collection<City> cities = user.getCities();

        if (cities != null) {
            if (!cities.contains(city)) {
                cities.add(city);
            }
        } else {
            user.setCities(new ArrayList<City>() {{
                add(city);
            }});
        }

        log.info("Cities " + user.getCities().toString());
        usersRepo.save(user);
    }

    public String getWeather(int cityId) {
        log.info("Build request with parameter (cityId)...");
        return doRequest(URI + "&id=" + cityId, citiesRepo.getOne(cityId));
    }

    private String doRequest(String uri, City city) {
        HttpUriRequest request = new HttpGet(uri);

        String weather = null;

        try {
            log.info("Send..");
            final HttpEntity entity = client.execute(request).getEntity();

            log.info("Parse data...");
            StringBuilder message = new StringBuilder();
            JSONObject obj = new JSONObject(EntityUtils.toString(entity));
            int infoCnt = 0;

            if (city.getName() != null || city.getState() != null) {
                String name = city.getName().equals(city.getState()) ? city.getName() :
                        String.format("%s, %s", city.getName(), city.getState());

                message.append(String.format("<b><i>%s</i></b>\n", name));

                // ТЕМПЕРАТУРА
                if (obj.has("main")) {
                    JSONObject main = obj.getJSONObject("main");

                    if (main.has("temp")) {
                        message.append(String.format(Locale.ENGLISH, "• Температура %.2f °C\n",
                                main.getFloat("temp")));
                        infoCnt++;
                    }

                    if (main.has("feels_like")) {
                        message.append(String.format(Locale.ENGLISH, "• Ощущается как %.2f °C\n",
                                main.getFloat("feels_like")));
                        infoCnt++;
                    }

                    // ВЛАЖНОСТЬ
                    if (main.has("humidity")) {
                        message.append(String.format("• Влажность %d %%\n",
                                main.getInt("humidity")));
                        infoCnt++;
                    }

                    // ДАВЛЕНИЕ
                    if (main.has("pressure")) {
                        int hPa = main.getInt("pressure");
                        message.append(String.format(Locale.ENGLISH, "• Давление %.2f мм рт.ст.\n",
                                hPa * 0.750063 - 17.2));
                        infoCnt++;
                    }
                }

                // ОПИСАНИЕ
                StringBuilder description = new StringBuilder();
                if (obj.has("weather")) {
                    JSONArray array = obj.getJSONArray("weather");

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject tmp = array.getJSONObject(i);
                        description.append(tmp.getString("description"));

                        if (i + 1 < array.length())
                            description.append(", ");
                    }

                    message.append(String.format("• На улице %s\n", description.toString()));
                    infoCnt++;
                }

                // ВЕТЕР
                if (obj.has("wind")) {
                    JSONObject wind = obj.getJSONObject("wind");

                    if (wind.has("speed")) {
                        message.append(String.format("• Скорость ветра %d м/с\n",
                                wind.getInt("speed")));
                        infoCnt++;
                    }
                    if (wind.has("deg")) {
                        message.append(String.format("• Направление ветра %s\n",
                                getWindDir((wind.getInt("deg")))));
                        infoCnt++;
                    }
                }

                // ОБЛАЧНОСТЬ
                if (obj.has("clouds")) {
                    JSONObject clouds = obj.getJSONObject("clouds");
                    message.append(String.format("• Облачность %d %%\n", clouds.getInt("all")));
                    infoCnt++;
                }

                // ВИДИМОСТЬ
                if (obj.has("visibility")) {
                    message.append(String.format("• Видимость %s м\n", df.format(obj.getInt("visibility"))));
                    infoCnt++;
                }

                if (infoCnt <= 2) {
                    weather = "Недостаточно данных для этого региона";
                } else {
                    weather = message.toString();
                }
            } else {
                weather = "Недостаточно данных для этого региона";
            }

            log.info("Parsing finish!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return weather;
    }

    private int getCityId(float lat, float lon) {
        String uri = String.format(URI + "&lat=%f&lon=%f", lat, lon);
        HttpUriRequest request = new HttpGet(uri);
        try {
            final HttpEntity entity = client.execute(request).getEntity();
            JSONObject obj = new JSONObject(EntityUtils.toString(entity));
            return obj.getInt("id");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
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

    private class LocationManager {

        private final String API_KEY = "&key=7ac3c3ab0d2117";
        private final String URL = "https://eu1.locationiq.com/v1/reverse.php?format=json&accept-language=ru" + API_KEY;

        private City getCity(float lat, float lon) {
            String uri = String.format(Locale.ENGLISH, URL + "&lat=%f&lon=%f", lat, lon);
            HttpUriRequest request = new HttpGet(uri);
            try {
                final HttpEntity entity = client.execute(request).getEntity();

                JSONObject body = new JSONObject(EntityUtils.toString(entity));
                log.info("Location manager: body is: " + body.toString());

                if (body.has("address")) {
                    JSONObject address = body.getJSONObject("address");

                    // NAME OF PLACE
                    String name;

                    if (address.has("village")) {
                        name = address.getString("village");
                    } else if (address.has("town")) {
                        name = address.getString("town");
                    } else if (address.has("city")) {
                        name = address.getString("city");
                    } else if (address.has("region")) {
                        name = address.getString("region");
                    } else if (address.has("county")) {
                        name = address.getString("county");
                    } else {
                        name = null;
                    }

                    if (name != null) {
                        if (name.contains("район")) {
                            name = name.replace("район", "р-н");
                        }
                    }

                    // SUBURB
                    String suburb;

                    if (address.has("neighbourhood")) {
                        suburb = address.getString("neighbourhood");
                    } else if (address.has("suburb")) {
                        suburb = address.getString("suburb");
                    } else if  (address.has("residential")) {
                        suburb = address.getString("residential");
                    } else {
                        suburb = null;
                    }

                    if (suburb != null) {
                        if (suburb.contains("микрорайон")) {
                            suburb = suburb.replace("микрорайон", "мкр.");
                        }
                    }

                    // STATE
                    String state;

                    if (address.has("state")) {
                        state = address.getString("state");
                    } else {
                        state = null;
                    }

                    if (state != null) {
                        if (state.contains("область")) {
                            state = state.replace("область", "обл.");
                        } else if (state.contains("административный округ")) {
                            state = state.replace("административный округ", "адм.о.");
                        }
                    }

                    City city = new City();
                    city.setName(name);
                    city.setState(state);
                    city.setSuburb(suburb);
                    city.setCountryCode(address.getString("country_code"));

                    return city;
                } else {
                    return null;
                }

            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
