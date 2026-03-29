package com.together.ideahub.dto;

import com.together.ideahub.entity.Category;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class IdeaDto {

    // ── Ответ: краткая карточка (для списка) ──────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Summary {
        private Long id;
        private String title;
        private Category category;
        private BigDecimal priceFrom;
        private Integer durationMin;
        private String location;
        private BigDecimal rating;
        private Integer reviewsCount;
        private String description;    // описание идеи
        private String coverPhotoUrl;      // первое фото
        private List<String> tags;
        private Boolean isUserCreated;
        private LocalDateTime createdAt;
    }

    // ── Ответ: детальная карточка ──────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Detail {
        private Long id;
        private String title;
        private String description;
        private Category category;
        private BigDecimal priceFrom;
        private Integer durationMin;
        private String location;
        private String address;
        private BigDecimal rating;
        private Integer reviewsCount;
        private Long authorUserId;
        private Boolean isUserCreated;
        private List<PhotoDto> photos;
        private List<String> tags;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // ── Запрос: создать / обновить идею ───────────────────────────────────
    @Data
    public static class CreateRequest {
        @NotBlank(message = "Title is required")
        @Size(max = 200)
        private String title;

        @Size(max = 5000)
        private String description;

        @NotNull(message = "Category is required")
        private Category category;

        @DecimalMin(value = "0.0")
        private BigDecimal priceFrom;

        @Min(1)
        private Integer durationMin;

        @Size(max = 100)
        private String location;

        @Size(max = 300)
        private String address;

        @Size(max = 10, message = "Max 10 tags")
        private Set<String> tags;
    }

    @Data
    public static class UpdateRequest {
        @Size(max = 200)
        private String title;

        @Size(max = 5000)
        private String description;

        private Category category;

        @DecimalMin(value = "0.0")
        private BigDecimal priceFrom;

        @Min(1)
        private Integer durationMin;

        @Size(max = 100)
        private String location;

        @Size(max = 300)
        private String address;

        @Size(max = 10)
        private Set<String> tags;
    }

    // ── Ответ: статус сохранения ───────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SaveStatus {
        private boolean saved;
    }
}