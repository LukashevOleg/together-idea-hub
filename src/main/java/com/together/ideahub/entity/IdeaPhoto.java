package com.together.ideahub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "idea_photos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IdeaPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idea_id", nullable = false)
    private Idea idea;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;              // ключ объекта в S3/MinIO

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;     // порядок отображения
}