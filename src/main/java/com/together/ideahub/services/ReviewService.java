package com.together.ideahub.services;

import com.together.ideahub.dto.ReviewDto;
import com.together.ideahub.entity.Review;
import com.together.ideahub.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    private final IdeaRatingService ideaRatingService;

    /**
     * Создать или обновить отзыв (пользователь может изменить свой отзыв)
     */
    @Transactional
    public ReviewDto.Response createOrUpdate(Long userId, ReviewDto.CreateRequest req) {
        if (req.getRating() == null || req.getRating() < 1 || req.getRating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Рейтинг должен быть от 1 до 5");
        }

        Review review = reviewRepository.findByUserIdAndIdeaId(userId, req.getIdeaId())
                .orElse(Review.builder()
                        .userId(userId)
                        .ideaId(req.getIdeaId())
                        .build());

        review.setIdeaTitle(req.getIdeaTitle());
        review.setRating(req.getRating());
        review.setComment(req.getComment());
        review.setAuthorName(req.getAuthorName());

        Review saved = reviewRepository.save(review);

        ideaRatingService.recalculate(req.getIdeaId());

        return toResponse(saved);
    }

    /**
     * Получить отзывы для идеи (постранично)
     */
    @Transactional(readOnly = true)
    public ReviewDto.PagedResponse getByIdea(Long ideaId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviewPage = reviewRepository.findByIdeaIdOrderByCreatedAtDesc(ideaId, pageable);

        Double avg = reviewRepository.findAverageRatingByIdeaId(ideaId);
        long count = reviewRepository.countByIdeaId(ideaId);

        return ReviewDto.PagedResponse.builder()
                .reviews(reviewPage.getContent().stream().map(this::toResponse).toList())
                .page(page)
                .size(size)
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .averageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : null)
                .reviewCount(count)
                .build();
    }

    /**
     * Сводка по идее: средний рейтинг + количество
     */
    @Transactional(readOnly = true)
    public ReviewDto.Summary getSummary(Long ideaId) {
        Double avg = reviewRepository.findAverageRatingByIdeaId(ideaId);
        long count = reviewRepository.countByIdeaId(ideaId);
        return ReviewDto.Summary.builder()
                .ideaId(ideaId)
                .averageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : null)
                .reviewCount(count)
                .build();
    }

    /**
     * Отзыв текущего пользователя на идею
     */
    @Transactional(readOnly = true)
    public ReviewDto.Response getMyReview(Long userId, Long ideaId) {
        return reviewRepository.findByUserIdAndIdeaId(userId, ideaId)
                .map(this::toResponse)
                .orElse(null);
    }

    private ReviewDto.Response toResponse(Review r) {
        return ReviewDto.Response.builder()
                .id(r.getId())
                .userId(r.getUserId())
                .ideaId(r.getIdeaId())
                .ideaTitle(r.getIdeaTitle())
                .rating(r.getRating())
                .comment(r.getComment())
                .authorName(r.getAuthorName())
                .createdAt(r.getCreatedAt())
                .build();
    }
}