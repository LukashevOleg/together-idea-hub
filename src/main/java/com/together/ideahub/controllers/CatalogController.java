package com.together.ideahub.controllers;

import com.together.ideahub.services.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    // GET /api/categories
    @GetMapping("/api/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(catalogService.getAllCategories());
    }

    // GET /api/tags
    @GetMapping("/api/tags")
    public ResponseEntity<List<String>> getTags() {
        return ResponseEntity.ok(catalogService.getAllTags());
    }
}