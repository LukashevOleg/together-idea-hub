package com.together.ideahub.repository;

import com.together.ideahub.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByIdeaIdOrderByCreatedAtDesc(Long ideaId, Pageable pageable);

    Optional<Review> findByUserIdAndIdeaId(Long userId, Long ideaId);

    boolean existsByUserIdAndIdeaId(Long userId, Long ideaId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.ideaId = :ideaId")
    Double findAverageRatingByIdeaId(@Param("ideaId") Long ideaId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.ideaId = :ideaId")
    long countByIdeaId(@Param("ideaId") Long ideaId);
}