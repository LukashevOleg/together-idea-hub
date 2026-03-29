package com.together.ideahub.dto;

import com.together.ideahub.entity.Idea;
import lombok.Builder;
import lombok.Getter;

/**
 * Идея с разбивкой итогового скора по компонентам.
 * Используется внутри сервисного слоя для ранжирования.
 */
@Getter
@Builder
public class ScoredIdea {

    private final Idea idea;

    /** Итоговый взвешенный скор [0, 1] */
    private final double totalScore;

    /** Компонент: соответствие предпочтениям пользователя */
    private final double prefScore;

    /** Компонент: соответствие текущему контексту (погода + время) */
    private final double contextScore;

    /** Компонент: социальный рейтинг (оценки + количество отзывов) */
    private final double socialScore;

    /** true если идея жёстко отфильтрована по контексту (плохая погода для outdoor) */
    private final boolean contextFiltered;

    public static ScoredIdea filtered(Idea idea) {
        return ScoredIdea.builder()
                .idea(idea)
                .totalScore(0.0)
                .contextFiltered(true)
                .build();
    }
}