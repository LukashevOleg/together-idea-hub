package com.together.ideahub.services;

import com.together.ideahub.dto.IdeaDto;
import com.together.ideahub.entity.Idea;
import com.together.ideahub.entity.IdeaSave;
import com.together.ideahub.exceptions.NotFoundException;
import com.together.ideahub.repository.IdeaRepository;
import com.together.ideahub.repository.IdeaSaveRepository;
import com.together.ideahub.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IdeaSaveService {

    private final IdeaSaveRepository saveRepo;
    private final IdeaRepository ideaRepo;
    private final S3StorageService storageService;

    // ── Лайкнуть ──────────────────────────────────────────────────────────

    @Transactional
    public IdeaDto.SaveStatus save(Long userId, Long ideaId) {
        Idea idea = ideaRepo.findById(ideaId)
                .orElseThrow(() -> new NotFoundException("Idea not found: " + ideaId));

        if (!saveRepo.existsByUserIdAndIdeaId(userId, ideaId)) {
            saveRepo.save(IdeaSave.builder().userId(userId).idea(idea).build());
        }

        return new IdeaDto.SaveStatus(true);
    }

    // ── Убрать лайк ───────────────────────────────────────────────────────

    @Transactional
    public IdeaDto.SaveStatus unsave(Long userId, Long ideaId) {
        saveRepo.deleteByUserIdAndIdeaId(userId, ideaId);
        return new IdeaDto.SaveStatus(false);
    }

    // ── Статус ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public IdeaDto.SaveStatus getStatus(Long userId, Long ideaId) {
        boolean saved = saveRepo.existsByUserIdAndIdeaId(userId, ideaId);
        return new IdeaDto.SaveStatus(saved);
    }

    // ── Список сохранённых (для DateModePage вкладка "Сохранённое") ───────

    @Transactional(readOnly = true)
    public List<IdeaDto.Summary> getSaved(Long userId) {
        return saveRepo.findByUserIdWithIdea(userId)
                .stream()
                .map(s -> toSummary(s.getIdea()))
                .toList();
    }

    // ── Mapper ─────────────────────────────────────────────────────────────

    private IdeaDto.Summary toSummary(Idea idea) {
        String coverUrl = idea.getPhotos().isEmpty() ? null
                : storageService.getPublicUrl(idea.getPhotos().iterator().next().getS3Key());

        return IdeaDto.Summary.builder()
                .id(idea.getId())
                .title(idea.getTitle())
                .category(idea.getCategory())
                .description(idea.getDescription())
                .priceFrom(idea.getPriceFrom())
                .durationMin(idea.getDurationMin())
                .location(idea.getLocation())
                .rating(idea.getRating())
                .reviewsCount(idea.getReviewsCount())
                .coverPhotoUrl(coverUrl)
                .tags(idea.getIdeaTags().stream().map(t -> t.getTag().getName()).toList())
                .isUserCreated(idea.getIsUserCreated())
                .createdAt(idea.getCreatedAt())
                .build();
    }
}