package com.together.ideahub.services;

import com.together.ideahub.repository.IdeaRepository;
import com.together.ideahub.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdeaRatingService {

    private final IdeaRepository ideaRepository;
    private final ReviewRepository reviewRepository;

    /**
     * Пересчитывает rating и reviewsCount у идеи на основе всех отзывов.
     * Вызывается из InternalRatingController после сохранения/изменения отзыва.
     */
    @Transactional
    public void recalculate(Long ideaId) {
        var idea = ideaRepository.findById(ideaId).orElse(null);
        if (idea == null) {
            log.warn("recalculate: idea {} not found", ideaId);
            return;
        }

        Double avg = reviewRepository.findAverageRatingByIdeaId(ideaId);
        long count = reviewRepository.countByIdeaId(ideaId);

        idea.setReviewsCount((int) count);
        idea.setRating(avg != null
                ? java.math.BigDecimal.valueOf(Math.round(avg * 100.0) / 100.0)
                : java.math.BigDecimal.ZERO);

        ideaRepository.save(idea);
        log.info("Recalculated ideaId={}: rating={}, reviewsCount={}", ideaId, idea.getRating(), count);
    }
}
