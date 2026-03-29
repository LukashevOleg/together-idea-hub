package com.together.ideahub.services;

import com.together.ideahub.dto.WeatherData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * Погода через Open-Meteo — полностью бесплатно, без API-ключа.
 *
 * Поток:
 *   1. Город → координаты (geocoding-api.open-meteo.com) — кэш 24ч
 *   2. Координаты → погода (api.open-meteo.com)          — кэш 30 мин
 *
 * WMO weather_code интерпретация:
 *   0        = ясно
 *   1-3      = переменная облачность
 *   45, 48   = туман
 *   51-67    = морось / дождь
 *   71-77    = снег
 *   80-82    = ливни
 *   95-99    = гроза
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String WEATHER_URL   = "https://api.open-meteo.com/v1/forecast";

    private final RestTemplate restTemplate;

    /**
     * Основной метод — принимает название города на русском или латинице.
     * Кэш погоды: 30 минут.
     */
    @Cacheable(value = "weatherCache", key = "#city.toLowerCase()")
    public WeatherData getWeather(String city) {
        try {
            double[] coords = resolveCoordinates(city);
            return fetchWeather(coords[0], coords[1]);
        } catch (Exception e) {
            log.error("Failed to get weather for city={}: {}", city, e.getMessage());
            return neutralWeather();
        }
    }

    /**
     * Геокодирование: город → [latitude, longitude].
     * Кэш координат: 24 часа (они не меняются).
     */
    @Cacheable(value = "geocodingCache", key = "#city.toLowerCase()")
    public double[] resolveCoordinates(String city) {
        String url = UriComponentsBuilder.fromHttpUrl(GEOCODING_URL)
                .queryParam("name", city)
                .queryParam("count", 1)
                .queryParam("language", "ru")
                .queryParam("format", "json")
                .toUriString();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response == null) {
            throw new RuntimeException("Empty geocoding response for city=" + city);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

        if (results == null || results.isEmpty()) {
            log.warn("City not found in geocoding: {}, falling back to Moscow", city);
            return new double[]{55.7558, 37.6173}; // Москва как дефолт
        }

        Map<String, Object> first = results.get(0);
        double lat = ((Number) first.get("latitude")).doubleValue();
        double lon = ((Number) first.get("longitude")).doubleValue();

        log.debug("Resolved city={} → lat={}, lon={}", city, lat, lon);
        return new double[]{lat, lon};
    }

    /**
     * Запрос погоды по координатам.
     */
    private WeatherData fetchWeather(double lat, double lon) {
        String url = UriComponentsBuilder.fromHttpUrl(WEATHER_URL)
                .queryParam("latitude",  lat)
                .queryParam("longitude", lon)
                .queryParam("current", "temperature_2m,precipitation,wind_speed_10m,weather_code")
                .queryParam("wind_speed_unit", "ms")  // м/с, не км/ч
                .queryParam("timezone", "auto")
                .toUriString();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response == null) {
            return neutralWeather();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> current = (Map<String, Object>) response.get("current");

        if (current == null) {
            return neutralWeather();
        }

        double temp        = ((Number) current.get("temperature_2m")).doubleValue();
        double windSpeed   = ((Number) current.get("wind_speed_10m")).doubleValue();
        double precip      = ((Number) current.get("precipitation")).doubleValue();
        int    weatherCode = ((Number) current.get("weather_code")).intValue();

        boolean hasPrecipitation = precip > 0.1 || weatherCode >= 51;
        String  condition        = resolveCondition(weatherCode);

        return WeatherData.builder()
                .temperatureCelsius(temp)
                .windSpeedMs(windSpeed)
                .precipitation(hasPrecipitation)
                .condition(condition)
                .build();
    }

    /**
     * WMO weather_code → строковый тип погоды для логов и отладки.
     */
    private String resolveCondition(int code) {
        if (code == 0)                    return "clear";
        if (code <= 3)                    return "clouds";
        if (code == 45 || code == 48)     return "mist";
        if (code >= 51  && code <= 67)    return "rain";
        if (code >= 71  && code <= 77)    return "snow";
        if (code >= 80  && code <= 82)    return "drizzle";
        if (code >= 95)                   return "thunderstorm";
        return "clouds";
    }

    private WeatherData neutralWeather() {
        return WeatherData.builder()
                .temperatureCelsius(20.0)
                .windSpeedMs(3.0)
                .precipitation(false)
                .condition("clear")
                .build();
    }
}

