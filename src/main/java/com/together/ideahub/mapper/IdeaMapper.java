package com.together.ideahub.mapper;

import com.together.ideahub.dto.IdeaResponseDto;
import com.together.ideahub.entity.Idea;
import com.together.ideahub.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class IdeaMapper {

    private final S3StorageService storageService;

    public IdeaResponseDto ideaToIdeaResponse(Idea idea) {
        var response = IdeaResponseDto.builder()
                .id(idea.getId())
                .title(idea.getTitle())
                .description(idea.getDescription())
                .category(idea.getCategory())
                .priceFrom(idea.getPriceFrom())
                .durationMin(idea.getDurationMin())
                .location(idea.getLocation())
                .address(idea.getAddress())
                .rating(idea.getRating())
                .reviewsCount(idea.getReviewsCount())
                .weatherCondition(idea.getWeatherCondition())
                .timeOfDay(idea.getTimeOfDay())
                .requiresBooking(idea.getRequiresBooking())
                .cityRelevance(idea.getCityRelevance())
                .authorUserId(idea.getAuthorUserId())
                .isUserCreated(idea.getIsUserCreated())
                .createdAt(idea.getCreatedAt())
                .updatedAt(idea.getUpdatedAt())
                .tags(idea.getIdeaTags() == null ? List.of() :
                        idea.getIdeaTags().stream()
                                .map(t -> t.getTag() != null ? t.getTag().getName() : null)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList())
                )
                .photos(idea.getPhotos() == null ? List.of() :
                        idea.getPhotos().stream()
                                .sorted(java.util.Comparator.comparingInt(
                                        p -> p.getSortOrder() != null ? p.getSortOrder() : 0))
                                .map(p -> IdeaResponseDto.PhotoDto.builder()
                                        .id(p.getId())
                                        .url(storageService.getPublicUrl(p.getS3Key()))
                                        .sortOrder(p.getSortOrder() != null ? p.getSortOrder() : 0)
                                        .build())
                                .collect(Collectors.toList())
                )
                .build();

        var coverPhotoUrl = response.getPhotos().isEmpty()
                ? null
                : response.getPhotos().get(0).getUrl();

        return response.toBuilder().coverPhotoUrl(coverPhotoUrl).build();
    }
}
