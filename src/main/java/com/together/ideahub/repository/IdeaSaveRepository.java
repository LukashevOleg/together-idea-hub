package com.together.ideahub.repository;

import com.together.ideahub.entity.IdeaSave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IdeaSaveRepository extends JpaRepository<IdeaSave, Long> {

    Optional<IdeaSave> findByUserIdAndIdeaId(Long userId, Long ideaId);

    boolean existsByUserIdAndIdeaId(Long userId, Long ideaId);

    void deleteByUserIdAndIdeaId(Long userId, Long ideaId);

    /** Все сохранённые идеи пользователя — от новых к старым */
    @Query("""
        SELECT s FROM IdeaSave s
        JOIN FETCH s.idea
        WHERE s.userId = :userId
        ORDER BY s.savedAt DESC
        """)
    List<IdeaSave> findByUserIdWithIdea(@Param("userId") Long userId);

    /** Кол-во сохранений идеи (для счётчика в карточке) */
    long countByIdeaId(Long ideaId);
}
