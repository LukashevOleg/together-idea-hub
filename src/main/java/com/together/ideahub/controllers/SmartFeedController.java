package com.together.ideahub.controllers;

import com.together.ideahub.dto.IdeaResponseDto;
import com.together.ideahub.dto.PageResponseDto;
import com.together.ideahub.dto.SmartFeedRequest;
import com.together.ideahub.entity.InteractionType;
import com.together.ideahub.mapper.IdeaMapper;
import com.together.ideahub.services.SmartFeedService;
import com.together.ideahub.services.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ideas")
@RequiredArgsConstructor
public class SmartFeedController {

    private final SmartFeedService smartFeedService;
    private final UserPreferenceService preferenceService;
    private final IdeaMapper ideaMapper;

    @GetMapping("/today")
    public ResponseEntity<PageResponseDto<IdeaResponseDto>> getTodayIdeas(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        SmartFeedRequest request = new SmartFeedRequest();
        request.setMode(SmartFeedRequest.FeedMode.SPONTANEOUS);
        request.setCity(city);
        request.setMaxPrice(maxPrice);
        request.setPage(page);
        request.setSize(size);

        return ResponseEntity.ok(
                PageResponseDto.from(smartFeedService.getSmartFeed(userId, request))
        );
    }

    @GetMapping("/smart")
    public ResponseEntity<PageResponseDto<IdeaResponseDto>> getSmartFeed(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Integer maxDurationMin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        SmartFeedRequest request = new SmartFeedRequest();
        request.setMode(SmartFeedRequest.FeedMode.PLANNED);
        request.setCity(city);
        request.setMaxPrice(maxPrice);
        request.setMaxDurationMin(maxDurationMin);
        request.setPage(page);
        request.setSize(size);

        return ResponseEntity.ok(PageResponseDto.from(smartFeedService.getSmartFeed(userId, request)));
    }

    @GetMapping("/swipe-pool")
    public ResponseEntity<List<IdeaResponseDto>> getSwipePool(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "50") int limit
    ) {
        var result = smartFeedService.getSwipePool(userId, city, limit);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{ideaId}/interact")
    public ResponseEntity<Map<String, String>> recordInteraction(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long ideaId,
            @RequestBody InteractionRequest body
    ) {
        preferenceService.recordInteraction(
                userId, ideaId, body.type(),
                body.viewDurationSeconds(), body.rating()
        );
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    record InteractionRequest(
            InteractionType type,
            Integer viewDurationSeconds,
            Integer rating
    ) {
    }
}