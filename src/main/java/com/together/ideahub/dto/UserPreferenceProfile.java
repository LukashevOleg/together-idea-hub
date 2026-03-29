package com.together.ideahub.dto;

import com.together.ideahub.entity.Category;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Профиль предпочтений пользователя, вычисляемый из истории взаимодействий.
 * Каждое значение — нормализованный вес в диапазоне [0.0, 1.0].
 */
@Getter
@Builder
public class UserPreferenceProfile {

    private final Long userId;

    /**
     * Вес предпочтения по категориям.
     * Вычисляется из: LIKE (+1.0), SKIP (-0.5), RATED (rating/5),
     * COMPLETED_DATE (+1.5), VIEWED > 30s (+0.2).
     * Нормализован в [0, 1] относительно всех категорий.
     */
    private final Map<Category, Double> categoryWeights;

    /**
     * Предпочтительный диапазон цен (из профиля или медианы лайков).
     */
    private final Double preferredPriceMax;

    /**
     * Предпочтительная длительность свидания в минутах (медиана из лайков).
     */
    private final Double preferredDurationMin;

    /**
     * Количество взаимодействий в базе — чем больше, тем точнее профиль.
     * При < 5 переключаемся на global popularity.
     */
    private final int totalInteractions;

    /**
     * Возвращает вес категории. Если категория не встречалась — 0.5 (нейтрально).
     */
    public double getCategoryWeight(Category category) {
        return categoryWeights.getOrDefault(category, 0.5);
    }

    public boolean isColdStart() {
        return totalInteractions < 5;
    }
}
