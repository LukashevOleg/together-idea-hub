package com.together.ideahub.controllers;

import com.together.ideahub.dto.ReviewDto;
import com.together.ideahub.services.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Создать / обновить отзыв.
     * Gateway кладёт userId в заголовок X-User-Id.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto.Response createOrUpdate(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName,
            @RequestBody ReviewDto.CreateRequest req
    ) {
        if (req.getAuthorName() == null && userName != null) {
            req.setAuthorName(userName);
        }
        return reviewService.createOrUpdate(userId, req);
    }

    /**
     * Отзывы для конкретной идеи.
     * GET /api/reviews/idea/42?page=0&size=5
     */
    @GetMapping("/idea/{ideaId}")
    public ReviewDto.PagedResponse getByIdea(
            @PathVariable Long ideaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return reviewService.getByIdea(ideaId, page, size);
    }

    /**
     * Сводка (средний рейтинг + количество) — для ленты.
     * GET /api/reviews/idea/42/summary
     */
    @GetMapping("/idea/{ideaId}/summary")
    public ReviewDto.Summary getSummary(@PathVariable Long ideaId) {
        return reviewService.getSummary(ideaId);
    }


    /**
     * Сводка (средний рейтинг + количество) — для ленты.
     * GET /api/reviews/idea/42/summary
     */
    @GetMapping("/idea/{ideaId}/all")
    public ReviewDto.AllIdeaReviewsResponse getAllIdeaReviews(@PathVariable Long ideaId) {
        return reviewService.getAllIdeaReviews(ideaId);
    }

    /**
     * Мой отзыв на идею (чтобы знать, ставил ли я уже оценку).
     * GET /api/reviews/idea/42/mine
     */
    @GetMapping("/idea/{ideaId}/mine")
    public ReviewDto.Response getMyReview(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long ideaId
    ) {
        return reviewService.getMyReview(userId, ideaId);
    }
}
