package com.together.ideahub.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Агрегированные данные погоды для нужд scoring.
 * Заполняется WeatherService из OpenWeatherMap (или любого другого API).
 */
@Getter
@Builder
public class WeatherData {

    /** Температура в °C */
    private final double temperatureCelsius;

    /** Скорость ветра м/с */
    private final double windSpeedMs;

    /** true если идут осадки (дождь, снег, гроза) */
    private final boolean precipitation;

    /** Описание погоды: "clear", "clouds", "rain", "snow", "thunderstorm", "drizzle", "mist" */
    private final String condition;

    /**
     * Пригодна ли погода для активного отдыха на улице.
     * Критерии: нет осадков, ветер < 10 м/с, температура 8–35°C.
     */
    public boolean isGoodForOutdoor() {
        return !precipitation
                && windSpeedMs < 10.0
                && temperatureCelsius >= 8.0
                && temperatureCelsius <= 35.0;
    }

    /**
     * Легкий выход на улицу (терпимая погода).
     * Нет сильных осадков, ветер < 15 м/с, температура 0–38°C.
     */
    public boolean isTolerableOutdoor() {
        return !precipitation
                && windSpeedMs < 15.0
                && temperatureCelsius >= 0.0
                && temperatureCelsius <= 38.0;
    }
}