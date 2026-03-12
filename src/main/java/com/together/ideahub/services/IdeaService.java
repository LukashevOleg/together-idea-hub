package com.together.ideahub.services;

import com.together.ideahub.dto.*;
import com.together.ideahub.entity.*;
import com.together.ideahub.exceptions.AccessDeniedException;
import com.together.ideahub.exceptions.NotFoundException;
import com.together.ideahub.repository.*;
import com.together.ideahub.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdeaService {

    private final IdeaRepository     ideaRepository;
    private final IdeaPhotoRepository photoRepository;
    private final TagRepository       tagRepository;
    private final S3StorageService    storageService;

    @Value("${storage.server-side-upload:true}")
    private boolean serverSideUpload;

    @Value("${ideas.page-size-max:100}")
    private int pageSizeMax;

    // ── GET /ideas ──────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PageResponse<IdeaDto.Summary> getIdeas(IdeaFilterRequest filter) {
        int size = Math.min(filter.getSize(), pageSizeMax);
        Sort sort = buildSort(filter.getSortBy(), filter.getSortDir());
        Pageable pageable = PageRequest.of(filter.getPage(), size, sort);

        Page<Idea> page = ideaRepository.findAll(
                IdeaSpecification.withFilters(filter), pageable);

        List<IdeaDto.Summary> content = page.getContent().stream()
                .map(this::toSummary)
                .toList();

        return PageResponse.<IdeaDto.Summary>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    // ── GET /ideas/{id} ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public IdeaDto.Detail getById(Long id) {
        Idea idea = ideaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Idea not found: " + id));
        return toDetail(idea);
    }

    // ── POST /ideas ─────────────────────────────────────────────────────────
    @Transactional
    public IdeaDto.Detail create(IdeaDto.CreateRequest req, Long authorUserId) {
        Idea idea = Idea.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .priceFrom(req.getPriceFrom())
                .durationMin(req.getDurationMin())
                .location(req.getLocation())
                .address(req.getAddress())
                .authorUserId(authorUserId)
                .isUserCreated(true)
                .build();

        attachTags(idea, req.getTags());
        Idea saved = ideaRepository.save(idea);
        log.info("Idea created: id={}, author={}", saved.getId(), authorUserId);
        return toDetail(saved);
    }

    // ── PUT /ideas/{id} ─────────────────────────────────────────────────────
    @Transactional
    public IdeaDto.Detail update(Long id, IdeaDto.UpdateRequest req, Long requestingUserId) {
        Idea idea = findAndCheckOwnership(id, requestingUserId);

        if (req.getTitle()       != null) idea.setTitle(req.getTitle());
        if (req.getDescription() != null) idea.setDescription(req.getDescription());
        if (req.getCategory()    != null) idea.setCategory(req.getCategory());
        if (req.getPriceFrom()   != null) idea.setPriceFrom(req.getPriceFrom());
        if (req.getDurationMin() != null) idea.setDurationMin(req.getDurationMin());
        if (req.getLocation()    != null) idea.setLocation(req.getLocation());
        if (req.getAddress()     != null) idea.setAddress(req.getAddress());

        if (req.getTags() != null) {
            idea.getIdeaTags().clear();
            attachTags(idea, req.getTags());
        }

        return toDetail(ideaRepository.save(idea));
    }

    // ── DELETE /ideas/{id} ──────────────────────────────────────────────────
    @Transactional
    public void delete(Long id, Long requestingUserId) {
        Idea idea = findAndCheckOwnership(id, requestingUserId);

        // Удаляем фото из S3
        idea.getPhotos().forEach(photo -> storageService.delete(photo.getS3Key()));

        ideaRepository.delete(idea);
        log.info("Idea deleted: id={}", id);
    }

    // ── POST /ideas/{id}/photos ─────────────────────────────────────────────
    @Transactional
    public PhotoUploadResponse uploadPhoto(Long ideaId, MultipartFile file,
                                           Long requestingUserId) throws IOException {
        Idea idea = findAndCheckOwnership(ideaId, requestingUserId);

        int nextOrder = idea.getPhotos().size();

        if (serverSideUpload) {
            // Загружаем файл через сервис → возвращаем публичный URL
            String url = storageService.upload(file, "ideas/" + ideaId);
            String s3Key = storageService.extractKeyFromUrl(url);

            IdeaPhoto photo = IdeaPhoto.builder()
                    .idea(idea)
                    .s3Key(s3Key)
                    .sortOrder(nextOrder)
                    .build();
            photoRepository.save(photo);

            return PhotoUploadResponse.builder()
                    .photoId(photo.getId())
                    .url(url)
                    .build();
        } else {
            // Генерируем presigned URL — фронт загружает напрямую в S3
            String filename = file.getOriginalFilename() != null
                    ? file.getOriginalFilename() : "photo.jpg";
            var result = storageService.generatePresignedUploadUrl(
                    "ideas/" + ideaId, filename, file.getContentType());

            // Сохраняем запись о фото заранее (фронт потом подтвердит)
            IdeaPhoto photo = IdeaPhoto.builder()
                    .idea(idea)
                    .s3Key(result.s3Key())
                    .sortOrder(nextOrder)
                    .build();
            photoRepository.save(photo);

            return PhotoUploadResponse.builder()
                    .photoId(photo.getId())
                    .presignedUrl(result.uploadUrl())
                    .url(result.publicUrl())
                    .build();
        }
    }

    // ── Хелперы ─────────────────────────────────────────────────────────────
    private Idea findAndCheckOwnership(Long ideaId, Long userId) {
        Idea idea = ideaRepository.findByIdWithDetails(ideaId)
                .orElseThrow(() -> new NotFoundException("Idea not found: " + ideaId));
        if (!Objects.equals(idea.getAuthorUserId(), userId)) {
            throw new AccessDeniedException("You don't own this idea");
        }
        return idea;
    }

    private void attachTags(Idea idea, Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return;

        List<Tag> existingTags = tagRepository.findByNameIn(tagNames);
        Map<String, Tag> tagMap = existingTags.stream()
                .collect(Collectors.toMap(Tag::getName, t -> t));

        tagNames.forEach(name -> {
            Tag tag = tagMap.computeIfAbsent(name, n -> tagRepository.save(
                    Tag.builder().name(n).build()));
            idea.getIdeaTags().add(IdeaTag.builder().idea(idea).tag(tag).build());
        });
    }

    private IdeaDto.Summary toSummary(Idea idea) {
        String coverUrl = idea.getPhotos().isEmpty() ? null
                : storageService.getPublicUrl(idea.getPhotos().get(0).getS3Key());

        return IdeaDto.Summary.builder()
                .id(idea.getId())
                .title(idea.getTitle())
                .category(idea.getCategory())
                .priceFrom(idea.getPriceFrom())
                .durationMin(idea.getDurationMin())
                .location(idea.getLocation())
                .rating(idea.getRating())
                .reviewsCount(idea.getReviewsCount())
                .coverPhotoUrl(coverUrl)
                .tags(idea.getIdeaTags().stream().map(it -> it.getTag().getName()).toList())
                .isUserCreated(idea.getIsUserCreated())
                .createdAt(idea.getCreatedAt())
                .build();
    }

    private IdeaDto.Detail toDetail(Idea idea) {
        List<PhotoDto> photos = idea.getPhotos().stream()
                .map(p -> new PhotoDto(p.getId(),
                        storageService.getPublicUrl(p.getS3Key()), p.getSortOrder()))
                .toList();

        return IdeaDto.Detail.builder()
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
                .authorUserId(idea.getAuthorUserId())
                .isUserCreated(idea.getIsUserCreated())
                .photos(photos)
                .tags(idea.getIdeaTags().stream().map(it -> it.getTag().getName()).toList())
                .createdAt(idea.getCreatedAt())
                .updatedAt(idea.getUpdatedAt())
                .build();
    }

    private Sort buildSort(String sortBy, String dir) {
        String field = switch (sortBy) {
            case "rating"    -> "rating";
            case "priceFrom" -> "priceFrom";
            default          -> "createdAt";
        };
        return "asc".equalsIgnoreCase(dir) ? Sort.by(field).ascending()
                : Sort.by(field).descending();
    }

}