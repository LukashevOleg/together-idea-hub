package com.together.ideahub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Отзыв пользователя на идею свидания.
 * Создаётся после завершения свидания (статус COMPLETED в dating).
 * Один пользователь — один отзыв на одну идею (UNIQUE userId + ideaId).
 */
@Entity
@Table(
        name = "reviews",
        schema = "ideahub",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "idea_id"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Кто оставил отзыв (userId из JWT) */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** На какую идею */
    @Column(name = "idea_id", nullable = false)
    private Long ideaId;

    /** Название идеи (денормализовано для удобства отображения) */
    @Column(name = "idea_title")
    private String ideaTitle;

    /** Оценка от 1 до 5 */
    @Column(nullable = false)
    private Integer rating;

    /** Текстовый комментарий (опционально) */
    @Column(length = 1000)
    private String comment;

    /** Имя автора (денормализовано) */
    @Column(name = "author_name")
    private String authorName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
