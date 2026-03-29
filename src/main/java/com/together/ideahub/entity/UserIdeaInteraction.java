package com.together.ideahub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_idea_interaction", indexes = {
        @Index(name = "idx_interaction_user_id",  columnList = "user_id"),
        @Index(name = "idx_interaction_idea_id",  columnList = "idea_id"),
        @Index(name = "idx_interaction_type",     columnList = "type"),
        @Index(name = "idx_interaction_user_idea",columnList = "user_id,idea_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserIdeaInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "idea_id")
    private Long ideaId;

    @Enumerated(EnumType.STRING)
    private InteractionType type;

    @Column(name = "view_duration_seconds")
    private Integer viewDurationSeconds;

    private Integer rating;
    // 1-5, только для RATED
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}