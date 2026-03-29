package com.together.ideahub.dto;

import com.together.ideahub.entity.Category;
import com.together.ideahub.entity.Idea;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Builder(toBuilder = true)
public class IdeaResponseDto {

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
    private String weatherCondition;
    private String timeOfDay;
    private Boolean requiresBooking;
    private String cityRelevance;
    private Long authorUserId;
    private String coverPhotoUrl;
    private Boolean isUserCreated;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<String> tags;       // просто строки, без обратной ссылки
    private List<PhotoDto> photos;

    // ── Вложенные DTO ─────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class PhotoDto {
        private Long id;
        private String url;
        private int sortOrder;
    }

}