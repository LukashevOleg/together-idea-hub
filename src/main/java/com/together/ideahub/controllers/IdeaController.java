package com.together.ideahub.controllers;

import com.together.ideahub.dto.*;
import com.together.ideahub.services.IdeaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * X-User-Id — заголовок, который API Gateway прокидывает после валидации JWT.
 * Сервис сам не занимается аутентификацией — это уже сделал Gateway.
 */
@RestController
@RequestMapping("/api/ideas")
@RequiredArgsConstructor
public class IdeaController {

    private final IdeaService ideaService;

    // GET /api/ideas?search=&category=&priceFrom=&priceTo=&tags=&page=&size=&sortBy=&sortDir=
    @GetMapping
    public ResponseEntity<PageResponse<IdeaDto.Summary>> getIdeas(
            @ModelAttribute IdeaFilterRequest filter) {
        return ResponseEntity.ok(ideaService.getIdeas(filter));
    }

    // GET /api/ideas/{id}
    @GetMapping("/{id}")
    public ResponseEntity<IdeaDto.Detail> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ideaService.getById(id));
    }

    // POST /api/ideas
    @PostMapping
    public ResponseEntity<IdeaDto.Detail> create(
            @Valid @RequestBody IdeaDto.CreateRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ideaService.create(request, userId));
    }

    // PUT /api/ideas/{id}
    @PutMapping("/{id}")
    public ResponseEntity<IdeaDto.Detail> update(
            @PathVariable Long id,
            @Valid @RequestBody IdeaDto.UpdateRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ideaService.update(id, request, userId));
    }

    // DELETE /api/ideas/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        ideaService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    // POST /api/ideas/{id}/photos
    @PostMapping("/{id}/photos")
    public ResponseEntity<PhotoUploadResponse> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") Long userId) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ideaService.uploadPhoto(id, file, userId));
    }
}
