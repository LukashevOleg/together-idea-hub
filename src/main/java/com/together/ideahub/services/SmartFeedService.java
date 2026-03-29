package com.together.ideahub.services;

import com.together.ideahub.dto.*;
import com.together.ideahub.entity.Category;
import com.together.ideahub.entity.Idea;
import com.together.ideahub.mapper.IdeaMapper;
import com.together.ideahub.repository.IdeaRepository;
import com.together.ideahub.repository.UserIdeaInteractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmartFeedService {

    private final IdeaRepository ideaRepository;
    private final UserIdeaInteractionRepository interactionRepository;
    private final WeatherService weatherService;
    private final UserPreferenceService preferenceService;
    private final IdeaScoringService scoringService;
    private final IdeaMapper ideaMapper;


    private static final int CANDIDATE_LIMIT = 200;

    public Page<IdeaResponseDto> getSmartFeed(Long userId, SmartFeedRequest request) {
        String city = resolveCity(userId, request.getCity());

        WeatherData weather = weatherService.getWeather(city);
        log.debug("Weather for city={}: temp={}°C, precip={}", city,
                weather.getTemperatureCelsius(), weather.isPrecipitation());

        UserPreferenceProfile profile = preferenceService.computeProfile(userId);

        // ── Шаг 1: кандидаты с мягкой фильтрацией ────────────────────────────
        List<Idea> candidates = fetchCandidates(request, weather);

        // Фолбэк А: если фильтры дали 0 — берём всё без погодных ограничений
        if (candidates.isEmpty()) {
            log.debug("SmartFeed: no candidates after filter, falling back to unfiltered");
            candidates = fetchAllCandidates(request);
        }

        // ── Шаг 2: исключаем уже виденные ─────────────────────────────────────
        Set<Long> excludeIds = getExcludedIdeaIds(userId, request.getMode());
        candidates = candidates.stream()
                .filter(idea -> !excludeIds.contains(idea.getId()))
                .collect(Collectors.toList());

        // ── Шаг 3: scoring ────────────────────────────────────────────────────
        List<ScoredIdea> scored = scoringService.scoreAndRank(
                candidates, profile, weather, request.getMode()
        );

        // Фолбэк Б: scoring вернул 0 (все отфильтрованы по контексту)
        // → показываем популярные идеи по предпочтительным категориям
        if (scored.isEmpty() && !candidates.isEmpty()) {
            log.debug("SmartFeed: scoring filtered all, falling back to category popular feed");
            scored = popularByPreferredCategories(candidates, profile);
        }

        // Фолбэк В: совсем нет данных → топ по рейтингу без персонализации
        if (scored.isEmpty()) {
            log.debug("SmartFeed: no results at all, falling back to global top");
            List<Idea> allIdeas = fetchAllCandidates(request);
            scored = popularByPreferredCategories(allIdeas, profile);
        }

        log.debug("SmartFeed userId={}: total scored={}, coldStart={}",
                userId, scored.size(), profile.isColdStart());

        // ── Шаг 4: пагинация ──────────────────────────────────────────────────
        return paginate(scored, request);
    }

    public List<IdeaResponseDto> getSwipePool(Long userId, String city, int limit) {
        SmartFeedRequest request = new SmartFeedRequest();
        request.setMode(SmartFeedRequest.FeedMode.PLANNED);
        request.setCity(city);
        request.setSize(limit);
        request.setPage(0);
        return getSmartFeed(userId, request).getContent().stream().toList();
    }

    // ── Популярные идеи по предпочтительным категориям ───────────────────────

    /**
     * Фолбэк когда scoring не даёт результатов.
     * <p>
     * Логика:
     * 1. Берём категории с весом > 0.4 из профиля (понравившиеся)
     * 2. Сортируем идеи: сначала из предпочитаемых категорий, потом остальные
     * 3. Внутри группы — по рейтингу
     * <p>
     * Для cold start (все веса = 0.5) все категории равны → просто по рейтингу.
     */
    private List<ScoredIdea> popularByPreferredCategories(
            List<Idea> ideas,
            UserPreferenceProfile profile
    ) {
        // Категории которые пользователю нравятся (вес > 0.4)
        Set<Category> preferred = profile.getCategoryWeights().entrySet().stream()
                .filter(e -> e.getValue() > 0.4)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        return ideas.stream()
                .sorted(Comparator
                        // Сначала предпочитаемые категории
                        .comparingInt((Idea idea) -> preferred.contains(idea.getCategory()) ? 0 : 1)
                        // Внутри группы — по рейтингу
                        .thenComparing(Comparator.comparingDouble(
                                (Idea idea) -> idea.getRating() != null ? idea.getRating().doubleValue() : 0.0
                        ).reversed())
                        // Потом по количеству отзывов
                        .thenComparing(Comparator.comparingInt(
                                (Idea idea) -> idea.getReviewsCount() != null ? idea.getReviewsCount() : 0
                        ).reversed())
                )
                .map(idea -> ScoredIdea.builder()
                        .idea(idea)
                        .totalScore(preferred.contains(idea.getCategory()) ? 0.6 : 0.4)
                        .prefScore(preferred.contains(idea.getCategory()) ? 0.8 : 0.3)
                        .contextScore(0.5)
                        .socialScore(idea.getRating() != null ? idea.getRating().doubleValue() / 5.0 : 0.0)
                        .contextFiltered(false)
                        .build()
                )
                .collect(Collectors.toList());
    }

    // ── Загрузка кандидатов ───────────────────────────────────────────────────

    /**
     * Кандидаты с погодной фильтрацией (основной путь).
     */
    private List<Idea> fetchCandidates(SmartFeedRequest request, WeatherData weather) {
        boolean spontaneous = request.getMode() == SmartFeedRequest.FeedMode.SPONTANEOUS;

        String excludeWeather = null;
        if (spontaneous && !weather.isTolerableOutdoor()) {
            excludeWeather = "OUTDOOR_ONLY";
        }

        BigDecimal maxPrice = request.getMaxPrice() != null
                ? BigDecimal.valueOf(request.getMaxPrice()) : null;

        return ideaRepository.findCandidatesForSmartFeed(
                spontaneous,
                excludeWeather,
                maxPrice,
                request.getMaxDurationMin(),
                PageRequest.of(0, CANDIDATE_LIMIT)
        );
    }

    /**
     * Кандидаты без каких-либо фильтров — фолбэк когда основной путь пуст.
     */
    private List<Idea> fetchAllCandidates(SmartFeedRequest request) {
        BigDecimal maxPrice = request.getMaxPrice() != null
                ? BigDecimal.valueOf(request.getMaxPrice()) : null;

        return ideaRepository.findCandidatesForSmartFeed(
                false,   // не исключаем requires_booking
                null,    // не исключаем никакую погоду
                maxPrice,
                request.getMaxDurationMin(),
                PageRequest.of(0, CANDIDATE_LIMIT)
        );
    }

    // ── Исключения ────────────────────────────────────────────────────────────

    private Set<Long> getExcludedIdeaIds(Long userId, SmartFeedRequest.FeedMode mode) {
        if (mode == SmartFeedRequest.FeedMode.SPONTANEOUS) {
            return new HashSet<>(interactionRepository.findExcludedIdeaIdsForSpontaneous(userId));
        } else {
            return new HashSet<>(interactionRepository.findExcludedIdeaIdsForPlanned(userId));
        }
    }

    // ── Пагинация ─────────────────────────────────────────────────────────────

    private Page<IdeaResponseDto> paginate(List<ScoredIdea> scored, SmartFeedRequest request) {
        int from = request.getPage() * request.getSize();
        int to = Math.min(from + request.getSize(), scored.size());

        List<IdeaResponseDto> content = (from >= scored.size())
                ? List.of()
                : scored.subList(from, to).stream()
                .map(ScoredIdea::getIdea)
                .map(ideaMapper::ideaToIdeaResponse)
                .toList();

        return new PageImpl<>(content, PageRequest.of(request.getPage(), request.getSize()), scored.size());
    }

    // ── Город ─────────────────────────────────────────────────────────────────

    private String resolveCity(Long userId, String requestCity) {
        if (requestCity != null && !requestCity.isBlank()) return requestCity;
        // TODO: вызов profiler-service
        return "Москва";
    }
}
