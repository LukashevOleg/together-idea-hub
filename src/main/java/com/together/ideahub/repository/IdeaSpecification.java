package com.together.ideahub.repository;

import com.together.ideahub.dto.IdeaFilterRequest;
import com.together.ideahub.entity.Idea;
import com.together.ideahub.entity.IdeaTag;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class IdeaSpecification {

    public static Specification<Idea> withFilters(IdeaFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Полнотекстовый поиск по title и description
            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                String pattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")),       pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            // Фильтр по категории
            if (filter.getCategory() != null) {
                predicates.add(cb.equal(root.get("category"), filter.getCategory()));
            }

            // Фильтр по цене (от/до)
            if (filter.getPriceFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("priceFrom"), filter.getPriceFrom()));
            }
            if (filter.getPriceTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("priceFrom"), filter.getPriceTo()));
            }

            // Фильтр по тегам (ANY — идея содержит хотя бы один из тегов)
            if (filter.getTags() != null && !filter.getTags().isEmpty()) {
                Join<Idea, IdeaTag> tagJoin = root.join("ideaTags", JoinType.INNER);
                predicates.add(tagJoin.get("tag").get("name").in(filter.getTags()));
                query.distinct(true);
            }

            // Фильтр по локации
            if (filter.getLocation() != null && !filter.getLocation().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("location")),
                        "%" + filter.getLocation().toLowerCase() + "%"
                ));
            }

            // Только пользовательские или только платформенные
            if (filter.getIsUserCreated() != null) {
                predicates.add(cb.equal(root.get("isUserCreated"), filter.getIsUserCreated()));
            }

            // Cursor-based пагинация для свайп-ленты
            if (filter.getAfterId() != null) {
                predicates.add(cb.greaterThan(root.get("id"), filter.getAfterId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}