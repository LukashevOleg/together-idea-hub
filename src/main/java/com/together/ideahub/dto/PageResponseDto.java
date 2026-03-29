package com.together.ideahub.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Универсальная обёртка для пагинированных ответов.
 * Заменяет Page<Entity> → Page<Dto> без утечки entity наружу.
 */
@Getter
@Builder
public class PageResponseDto<T> {

    private List<T> content;
    private int     page;
    private int     size;
    private long    totalElements;
    private int     totalPages;
    private boolean last;
    private boolean first;

    public static <E> PageResponseDto<E> from(Page<E> page) {
        return PageResponseDto.<E>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }

}