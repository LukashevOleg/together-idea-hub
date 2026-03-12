package com.together.ideahub.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PhotoDto {
    private Long id;
    private String url;
    private Integer sortOrder;
}