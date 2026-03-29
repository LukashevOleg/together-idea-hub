package com.together.ideahub.dto;

import lombok.Data;

/**
 * Параметры запроса умной ленты / идей на сегодня.
 */
@Data
public class SmartFeedRequest {

    /**
     * Режим: SPONTANEOUS (сегодня) или PLANNED (на дату).
     */
    private FeedMode mode = FeedMode.SPONTANEOUS;

    /**
     * Город пользователя для погоды и геофильтрации.
     * Если null — берётся из профиля.
     */
    private String city;

    /** Максимальный бюджет (null = без ограничений) */
    private Double maxPrice;

    /** Максимальная длительность в минутах (null = без ограничений) */
    private Integer maxDurationMin;

    /** Размер страницы */
    private int size = 20;

    /** Номер страницы */
    private int page = 0;

    public enum FeedMode {
        SPONTANEOUS,  // Спонтанное — с учётом погоды прямо сейчас, без брони
        PLANNED       // Плановое — умный подбор без жёстких погодных фильтров
    }
}
