package com.together.ideahub.services;

import com.together.ideahub.dto.UserPreferenceProfile;
import com.together.ideahub.entity.Category;
import com.together.ideahub.entity.Idea;
import com.together.ideahub.entity.InteractionType;
import com.together.ideahub.entity.UserIdeaInteraction;
import com.together.ideahub.repository.IdeaRepository;
import com.together.ideahub.repository.UserIdeaInteractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceService {

    private final UserIdeaInteractionRepository interactionRepository;
    private final IdeaRepository ideaRepository;

    private static final double W_LIKE = 1.0;
    private static final double W_SKIP = -0.5;
    private static final double W_VIEWED = 0.2;
    private static final double W_COMPLETED = 1.5;
    private static final double W_RATED = 1.2;

    // ── Запись взаимодействия ─────────────────────────────────────────────────

    /**
     * Сохраняет взаимодействие пользователя с идеей (upsert по userId+ideaId+type).
     * После сохранения инвалидирует кэш профиля этого пользователя.
     */
    @CacheEvict(value = "userPreferenceCache", key = "#userId")
    public void recordInteraction(Long userId, Long ideaId, InteractionType type,
                                  Integer viewDurationSeconds, Integer rating) {
        UserIdeaInteraction interaction = interactionRepository
                .findByUserIdAndIdeaIdAndType(userId, ideaId, type)
                .orElse(UserIdeaInteraction.builder()
                        .userId(userId)
                        .ideaId(ideaId)
                        .type(type)
                        .build());

        interaction.setViewDurationSeconds(viewDurationSeconds);
        interaction.setRating(rating);

        interactionRepository.save(interaction);

        log.debug("Recorded interaction: userId={}, ideaId={}, type={}", userId, ideaId, type);
    }

    // ── Профиль предпочтений ──────────────────────────────────────────────────

    @Cacheable(value = "userPreferenceCache", key = "#userId")
    public UserPreferenceProfile computeProfile(Long userId) {
        List<UserIdeaInteraction> interactions = interactionRepository.findByUserId(userId);

        if (interactions.isEmpty()) {
            return coldStartProfile(userId);
        }

        Set<Long> ideaIds = interactions.stream()
                .map(UserIdeaInteraction::getIdeaId)
                .collect(Collectors.toSet());

        Map<Long, Idea> ideaMap = ideaRepository.findAllById(ideaIds).stream()
                .collect(Collectors.toMap(Idea::getId, i -> i));

        Map<Category, Double> rawScores = new EnumMap<>(Category.class);
        List<Double> likedPrices = new ArrayList<>();
        List<Integer> likedDurations = new ArrayList<>();

        for (UserIdeaInteraction interaction : interactions) {
            Idea idea = ideaMap.get(interaction.getIdeaId());
            if (idea == null) continue;

            double delta = signalWeight(interaction);
            if (delta == 0.0) continue;

            rawScores.merge(idea.getCategory(), delta, Double::sum);

            if (interaction.getType() == InteractionType.LIKE
                    || interaction.getType() == InteractionType.COMPLETED_DATE) {
                if (idea.getPriceFrom() != null) likedPrices.add(idea.getPriceFrom().doubleValue());
                if (idea.getDurationMin() != null) likedDurations.add(idea.getDurationMin());
            }
        }

        return UserPreferenceProfile.builder()
                .userId(userId)
                .categoryWeights(normalize(rawScores))
                .preferredPriceMax(percentile(likedPrices, 75))
                .preferredDurationMin(percentile(
                        likedDurations.stream().map(Double::valueOf).toList(), 75))
                .totalInteractions(interactions.size())
                .build();
    }

    // ── Приватные методы ──────────────────────────────────────────────────────

    private double signalWeight(UserIdeaInteraction i) {
        return switch (i.getType()) {
            case LIKE -> W_LIKE;
            case SKIP -> W_SKIP;
            case COMPLETED_DATE -> W_COMPLETED;
            case RATED -> i.getRating() != null ? (i.getRating() / 5.0) * W_RATED : 0.0;
            case VIEWED -> {
                Integer dur = i.getViewDurationSeconds();
                yield (dur != null && dur > 30) ? W_VIEWED : 0.0;
            }
        };
    }

    private Map<Category, Double> normalize(Map<Category, Double> raw) {
        if (raw.isEmpty()) return Collections.emptyMap();

        double min = raw.values().stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = raw.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double range = max - min;

        if (range == 0) {
            return raw.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> 0.5));
        }

        return raw.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (e.getValue() - min) / range
                ));
    }

    private Double percentile(List<Double> values, int pct) {
        if (values.isEmpty()) return null;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int idx = (int) Math.ceil(pct / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, idx));
    }

    private UserPreferenceProfile coldStartProfile(Long userId) {
        log.debug("Cold start profile for userId={}", userId);
        Map<Category, Double> neutral = new EnumMap<>(Category.class);
        for (Category cat : Category.values()) neutral.put(cat, 0.5);

        return UserPreferenceProfile.builder()
                .userId(userId)
                .categoryWeights(neutral)
                .preferredPriceMax(null)
                .preferredDurationMin(null)
                .totalInteractions(0)
                .build();
    }
}