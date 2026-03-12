package com.together.ideahub.services;

import com.together.ideahub.entity.Category;
import com.together.ideahub.entity.Tag;
import com.together.ideahub.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final TagRepository tagRepository;

    // GET /tags
    @Transactional(readOnly = true)
    public List<String> getAllTags() {
        return tagRepository.findAll().stream()
                .map(Tag::getName)
                .sorted()
                .toList();
    }

    // GET /categories
    public List<String> getAllCategories() {
        return Arrays.stream(Category.values())
                .map(Enum::name)
                .toList();
    }
}
