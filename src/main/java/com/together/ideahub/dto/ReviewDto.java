package com.together.ideahub.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class ReviewDto {

    /** Запрос на создание отзыва */
    @Data
    public static class CreateRequest {
        private Long ideaId;
        private String ideaTitle;
        private Integer rating;   // 1–5
        private String comment;
        private String authorName;
    }

    /** Один отзыв в ответе */
    @Data @Builder
    public static class Response {
        private Long id;
        private Long userId;
        private Long ideaId;
        private String ideaTitle;
        private Integer rating;
        private String comment;
        private String authorName;
        private LocalDateTime createdAt;
    }

    /** Сводка по идее: средний рейтинг + количество отзывов */
    @Data @Builder
    public static class Summary {
        private Long ideaId;
        private Double averageRating;   // null если отзывов нет
        private long reviewCount;
    }

    /** Страница отзывов + сводка */
    @Data @Builder
    public static class PagedResponse {
        private List<Response> reviews;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private Double averageRating;
        private long reviewCount;
    }
}