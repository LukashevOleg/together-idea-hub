package com.together.ideahub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "ideas", indexes = {
        @Index(name = "idx_ideas_category",      columnList = "category"),
        @Index(name = "idx_ideas_author",        columnList = "author_user_id"),
        @Index(name = "idx_ideas_is_user_created", columnList = "is_user_created"),
        @Index(name = "idx_ideas_rating",        columnList = "rating")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Idea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(name = "price_from", precision = 10, scale = 2)
    private BigDecimal priceFrom;

    @Column(name = "duration_min")
    private Integer durationMin;       // длительность в минутах

    @Column(length = 100)
    private String location;           // город / регион

    @Column(length = 300)
    private String address;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "reviews_count")
    @Builder.Default
    private Integer reviewsCount = 0;

    @Column(name = "author_user_id")
    private Long authorUserId;         // ID из auth-service

    @Column(name = "is_user_created", nullable = false)
    @Builder.Default
    private Boolean isUserCreated = false;  // false = контент платформы, true = от пользователя

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Связи ───────────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "idea", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private Set<IdeaPhoto> photos = new HashSet<>();

    @OneToMany(mappedBy = "idea", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<IdeaTag> ideaTags = new HashSet<>();

}