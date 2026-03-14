package com.together.ideahub.dto;

import com.together.ideahub.entity.Category;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;

@Data
public class IdeaFilterRequest {
    private String search;
    private Category category;
    private BigDecimal priceFrom;
    private BigDecimal priceTo;
    private Set<String> tags;
    private String location;
    private Boolean isUserCreated;

    /**
     * Курсор для свайп-ленты (cursor-based pagination).
     * Если передан — возвращаем только идеи с id > afterId.
     * Гарантирует что пользователь не увидит уже просмотренные идеи.
     */
    private Long afterId;

    private int page = 0;
    private int size = 20;
    private String sortBy  = "createdAt";  // createdAt | rating | priceFrom
    private String sortDir = "desc";
}
