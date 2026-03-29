package com.together.ideahub.repository;

import com.together.ideahub.entity.Category;
import com.together.ideahub.entity.Idea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface IdeaRepository extends JpaRepository<Idea, Long>,
        JpaSpecificationExecutor<Idea> {

    // Детальная карточка — сразу подтягиваем фото и теги одним запросом
    @Query("""
        SELECT DISTINCT i FROM Idea i
        LEFT JOIN FETCH i.photos
        LEFT JOIN FETCH i.ideaTags it
        LEFT JOIN FETCH it.tag
        WHERE i.id = :id
    """)
    Optional<Idea> findByIdWithDetails(@Param("id") Long id);

    // Все идеи автора
    Page<Idea> findByAuthorUserIdOrderByCreatedAtDesc(Long authorUserId, Pageable pageable);

    Page<Idea> findByCategory(Category category, Pageable pageable);

    // ── Умная лента: предфильтрация кандидатов ───────────────────────────────

    /**
     * Грубая предфильтрация кандидатов для scoring.
     *
     * Параметры:
     *   excludeBookingRequired — если true, исключаем ideas с requires_booking=true
     *   excludeWeatherCondition — если не null, исключаем идеи с этим weatherCondition
     *   maxPrice — если не null, фильтруем по price_from
     *   maxDuration — если не null, фильтруем по duration_min
     *
     * Сортировка: rating DESC — для начального порядка, потом scoring пересортирует.
     */
    @Query("""
        SELECT i FROM Idea i
        WHERE
            (:excludeBookingRequired = false OR i.requiresBooking IS NULL OR i.requiresBooking = false)
            AND (:excludeWeatherCondition IS NULL OR i.weatherCondition != :excludeWeatherCondition)
            AND (:maxPrice IS NULL OR i.priceFrom IS NULL OR i.priceFrom <= :maxPrice)
            AND (:maxDuration IS NULL OR i.durationMin IS NULL OR i.durationMin <= :maxDuration)
        ORDER BY i.rating DESC, i.reviewsCount DESC
        """)
    List<Idea> findCandidatesForSmartFeed(
            @Param("excludeBookingRequired") boolean excludeBookingRequired,
            @Param("excludeWeatherCondition") String excludeWeatherCondition,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("maxDuration") Integer maxDuration,
            Pageable pageable
    );

    // ── Поиск по тексту и фильтрам (для обычной ленты) ───────────────────────

    @Query("""
        SELECT i FROM Idea i
        WHERE
            (:category IS NULL OR i.category = :category)
            AND (:search IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(i.description) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY
            CASE WHEN :sortBy = 'rating'    THEN i.rating    END DESC,
            CASE WHEN :sortBy = 'createdAt' THEN i.createdAt END DESC,
            CASE WHEN :sortBy = 'priceFrom' THEN i.priceFrom END ASC
        """)
    Page<Idea> findByFilters(
            @Param("category")  Category category,
            @Param("search")    String search,
            @Param("sortBy")    String sortBy,
            Pageable            pageable
    );
}