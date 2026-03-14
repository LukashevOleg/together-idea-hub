package com.together.ideahub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Лайк/сохранение идеи пользователем.
 * Уникальный индекс (userId, idea) — нельзя лайкнуть дважды.
 */
@Entity
@Table(
        name = "idea_saves",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_idea_save_user_idea",
                columnNames = {"userId", "idea_id"}
        )
)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class IdeaSave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idea_id", nullable = false)
    private Idea idea;

    @CreationTimestamp
    private LocalDateTime savedAt;
}