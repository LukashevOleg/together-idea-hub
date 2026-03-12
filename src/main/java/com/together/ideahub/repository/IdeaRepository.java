package com.together.ideahub.repository;

import com.together.ideahub.entity.Idea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}