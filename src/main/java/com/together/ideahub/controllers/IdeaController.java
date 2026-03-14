package com.together.ideahub.controllers;

import com.together.ideahub.dto.*;
import com.together.ideahub.services.IdeaService;
import com.together.ideahub.services.IdeaSaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * X-User-Id — заголовок, который API Gateway прокидывает после валидации JWT.
 * Сервис сам не занимается аутентификацией — это уже сделал Gateway.
 */
@RestController
@RequestMapping("/api/ideas")
@RequiredArgsConstructor
public class IdeaController {

    private final IdeaService     ideaService;
    private final IdeaSaveService saveService;

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

    // GET /api/ideas/mine — только идеи текущего пользователя (требует JWT → X-User-Id)
    @GetMapping("/mine")
    public ResponseEntity<PageResponse<IdeaDto.Summary>> getMyIdeas(
            @RequestHeader("X-User-Id") Long userId,
            @ModelAttribute IdeaFilterRequest filter) {
        filter.setAuthorUserId(userId);
        filter.setIsUserCreated(true);
        return ResponseEntity.ok(ideaService.getIdeas(filter));
    }

    // ── Сохранить/лайкнуть идею ───────────────────────────────────────────

    // POST /api/ideas/{id}/save
    @PostMapping("/{id}/save")
    public ResponseEntity<IdeaDto.SaveStatus> save(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(saveService.save(userId, id));
    }

    // DELETE /api/ideas/{id}/save
    @DeleteMapping("/{id}/save")
    public ResponseEntity<IdeaDto.SaveStatus> unsave(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(saveService.unsave(userId, id));
    }

    // GET /api/ideas/{id}/save
    @GetMapping("/{id}/save")
    public ResponseEntity<IdeaDto.SaveStatus> saveStatus(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(saveService.getStatus(userId, id));
    }

    // GET /api/ideas/saved  — список сохранённых текущим пользователем
    @GetMapping("/saved")
    public ResponseEntity<List<IdeaDto.Summary>> getSaved(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(saveService.getSaved(userId));
    }
}