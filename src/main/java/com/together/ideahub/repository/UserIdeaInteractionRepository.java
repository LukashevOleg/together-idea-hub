package com.together.ideahub.repository;

import com.together.ideahub.entity.UserIdeaInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserIdeaInteractionRepository extends JpaRepository<UserIdeaInteraction, Long> {

    /** Все взаимодействия пользователя — для вычисления профиля предпочтений */
    List<UserIdeaInteraction> findByUserId(Long userId);

    /**
     * ID идей, исключаемых из спонтанной ленты:
     *   - LIKE / COMPLETED (уже нравятся, не повторяем)
     *   - SKIP за последние 7 дней (потом можно показать снова)
     */
    @Query("""
        SELECT u.ideaId FROM UserIdeaInteraction u
        WHERE u.userId = :userId
          AND (
              u.type IN ('LIKE', 'COMPLETED_DATE')
              OR (u.type = 'SKIP' AND u.createdAt >= CURRENT_TIMESTAMP - 7 DAY)
          )
        """)
    List<Long> findExcludedIdeaIdsForSpontaneous(@Param("userId") Long userId);

    /**
     * ID идей, исключаемых из плановой ленты:
     *   - только LIKE и COMPLETED (не повторяем уже понравившиеся)
     *   - SKIP не исключаем — пользователь мог передумать
     */
    @Query("""
        SELECT u.ideaId FROM UserIdeaInteraction u
        WHERE u.userId = :userId
          AND u.type IN ('LIKE', 'COMPLETED_DATE')
        """)
    List<Long> findExcludedIdeaIdsForPlanned(@Param("userId") Long userId);

    /**
     * Сохраняет или обновляет взаимодействие.
     * Если уже есть запись userId + ideaId + type — обновляем (upsert).
     */
    @Query("""
        SELECT u FROM UserIdeaInteraction u
        WHERE u.userId = :userId AND u.ideaId = :ideaId AND u.type = :type
        """)
    java.util.Optional<UserIdeaInteraction> findByUserIdAndIdeaIdAndType(
            @Param("userId")  Long userId,
            @Param("ideaId")  Long ideaId,
            @Param("type")    com.together.ideahub.entity.InteractionType type
    );
}

