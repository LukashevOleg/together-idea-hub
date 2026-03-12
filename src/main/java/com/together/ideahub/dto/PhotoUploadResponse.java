package com.together.ideahub.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PhotoUploadResponse {
    private Long photoId;
    private String url;            // публичный URL после загрузки
    private String presignedUrl;   // если server-side-upload=false
}