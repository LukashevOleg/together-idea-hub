package com.together.ideahub.services;

import com.together.ideahub.dto.ScoredIdea;
import com.together.ideahub.dto.UserPreferenceProfile;
import com.together.ideahub.dto.WeatherData;
import com.together.ideahub.dto.SmartFeedRequest;
import com.together.ideahub.entity.Idea;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

/**
 * Вычисляет итоговый скор для идеи по формуле:
 *
 *   score = W_PREF * prefScore
 *         + W_CTX  * contextScore
 *         + W_SOC  * socialScore
 *
 * Веса зависят от режима:
 *   SPONTANEOUS — контекст важнее (погода, время, нет брони)
 *   PLANNED     — предпочтения важнее, контекст мягче
 *
 * Диапазоны всех sub-score: [0.0, 1.0]
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdeaScoringService {

    // ── Веса для SPONTANEOUS режима ──────────────────────────────────────────
    private static final double SP_W_PREF = 0.35;
    private static final double SP_W_CTX  = 0.45;
    private static final double SP_W_SOC  = 0.20;

    // ── Веса для PLANNED режима ──────────────────────────────────────────────
    private static final double PL_W_PREF = 0.50;
    private static final double PL_W_CTX  = 0.20;
    private static final double PL_W_SOC  = 0.30;

    // ── Порог рейтинга для нормализации ─────────────────────────────────────
    private static final double MAX_RATING = 5.0;
    private static final double MAX_REVIEWS = 200.0; // reviews_count для насыщения

    /**
     * Оценивает одну идею.
     * Возвращает ScoredIdea.filtered() если идея жёстко исключена по контексту.
     */
    public ScoredIdea score(
            Idea idea,
            UserPreferenceProfile profile,
            WeatherData weather,
            SmartFeedRequest.FeedMode mode
    ) {
        boolean spontaneous = mode == SmartFeedRequest.FeedMode.SPONTANEOUS;

        // ── Жёсткая контекстная фильтрация (только для спонтанного) ─────────
        if (spontaneous && isHardFiltered(idea, weather)) {
            return ScoredIdea.filtered(idea);
        }

        double prefScore    = computePrefScore(idea, profile);
        double contextScore = computeContextScore(idea, weather, spontaneous);
        double socialScore  = computeSocialScore(idea);

        double total;
        if (spontaneous) {
            total = SP_W_PREF * prefScore + SP_W_CTX * contextScore + SP_W_SOC * socialScore;
        } else {
            total = PL_W_PREF * prefScore + PL_W_CTX * contextScore + PL_W_SOC * socialScore;
        }

        return ScoredIdea.builder()
                .idea(idea)
                .totalScore(clamp(total))
                .prefScore(prefScore)
                .contextScore(contextScore)
                .socialScore(socialScore)
                .contextFiltered(false)
                .build();
    }

    /** Оценивает список идей и возвращает отсортированный по totalScore */
    public List<ScoredIdea> scoreAndRank(
            List<Idea> ideas,
            UserPreferenceProfile profile,
            WeatherData weather,
            SmartFeedRequest.FeedMode mode
    ) {
        return ideas.stream()
                .map(idea -> score(idea, profile, weather, mode))
                .filter(si -> !si.isContextFiltered())
                .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
                .toList();
    }

    // ── Жёсткие фильтры ─────────────────────────────────────────────────────

    /**
     * Жёсткое исключение идеи из спонтанной ленты.
     * Примеры: outdoor при ливне, требует брони.
     */
    private boolean isHardFiltered(Idea idea, WeatherData weather) {
        // Идея только на улице, а погода плохая
        if ("OUTDOOR_ONLY".equals(idea.getWeatherCondition()) && !weather.isTolerableOutdoor()) {
            return true;
        }
        // В спонтанном режиме нельзя брать идеи с обязательной бронью
        if (Boolean.TRUE.equals(idea.getRequiresBooking())) {
            return true;
        }
        return false;
    }

    // ── Компоненты скора ─────────────────────────────────────────────────────

    /**
     * prefScore — насколько идея совпадает с предпочтениями пользователя.
     *
     * Базис: categoryWeight из профиля (обучается на лайках/скипах).
     * Бонусы:
     *   +0.1  если цена в пределах предпочтительного диапазона
     *   +0.05 если длительность близка к предпочтительной (±30 мин)
     */
    private double computePrefScore(Idea idea, UserPreferenceProfile profile) {
        double base = profile.getCategoryWeight(idea.getCategory()); // [0, 1]

        double bonus = 0.0;

        // Бонус за цену
        if (profile.getPreferredPriceMax() != null && idea.getPriceFrom() != null) {
            double price = idea.getPriceFrom().doubleValue();
            if (price <= profile.getPreferredPriceMax()) {
                bonus += 0.10;
            } else {
                // Плавный штраф за превышение бюджета
                double overshoot = (price - profile.getPreferredPriceMax())
                        / profile.getPreferredPriceMax();
                bonus -= Math.min(0.15, overshoot * 0.15);
            }
        }

        // Бонус за длительность
        if (profile.getPreferredDurationMin() != null && idea.getDurationMin() != null) {
            double diff = Math.abs(idea.getDurationMin() - profile.getPreferredDurationMin());
            if (diff <= 30) bonus += 0.05;
        }

        return clamp(base + bonus);
    }

    /**
     * contextScore — насколько идея подходит прямо сейчас.
     *
     * Учитывает:
     *   - Соответствие погоды (outdoor / indoor / any)
     *   - Время суток
     *   - Длительность (в спонтанном режиме — короткие лучше)
     */
    private double computeContextScore(Idea idea, WeatherData weather, boolean spontaneous) {
        double score = 0.5; // нейтральный старт

        // ── Погода ───────────────────────────────────────────────────────────
        String wc = idea.getWeatherCondition();
        if (wc != null) {
            switch (wc) {
                case "OUTDOOR_ONLY" -> {
                    if (weather.isGoodForOutdoor())    score += 0.30;
                    else if (weather.isTolerableOutdoor()) score += 0.10;
                    else                               score -= 0.30;
                }
                case "INDOOR_ONLY"  -> {
                    // Дождь — бонус для indoor
                    if (weather.isPrecipitation())     score += 0.20;
                    else                               score += 0.05;
                }
                case "ANY"          -> score += 0.05; // небольшой бонус за универсальность
            }
        }

        // ── Время суток ──────────────────────────────────────────────────────
        String tod = idea.getTimeOfDay();
        if (tod != null && !tod.equals("ANY")) {
            String currentTod = currentTimeOfDay();
            if (tod.equals(currentTod)) {
                score += 0.20;
            } else {
                score -= 0.15; // идея не для этого времени
            }
        }

        // ── Длительность (спонтанный режим предпочитает короткие) ───────────
        if (spontaneous && idea.getDurationMin() != null) {
            int dur = idea.getDurationMin();
            if      (dur <= 60)  score += 0.15;
            else if (dur <= 120) score += 0.05;
            else if (dur > 240)  score -= 0.10;
        }

        return clamp(score);
    }

    /**
     * socialScore — популярность идеи среди всех пользователей.
     *
     * Формула: 0.6 * normalizedRating + 0.4 * normalizedReviews
     * Нормализация reviews: log(count+1) / log(MAX_REVIEWS+1)
     */
    private double computeSocialScore(Idea idea) {
        double ratingNorm = idea.getRating() != null
                ? idea.getRating().doubleValue() / MAX_RATING
                : 0.0;

        double reviewsNorm = 0.0;
        if (idea.getReviewsCount() != null && idea.getReviewsCount() > 0) {
            reviewsNorm = Math.log(idea.getReviewsCount() + 1)
                    / Math.log(MAX_REVIEWS + 1);
        }

        return clamp(0.6 * ratingNorm + 0.4 * reviewsNorm);
    }

    // ── Утилиты ──────────────────────────────────────────────────────────────

    private String currentTimeOfDay() {
        LocalTime now = LocalTime.now();
        if      (now.isBefore(LocalTime.of(12, 0))) return "MORNING";
        else if (now.isBefore(LocalTime.of(18, 0))) return "AFTERNOON";
        else                                         return "EVENING";
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
