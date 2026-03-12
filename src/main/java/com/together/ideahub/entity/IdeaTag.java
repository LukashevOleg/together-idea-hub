package com.together.ideahub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "idea_tags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"idea_id", "tag_id"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IdeaTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idea_id", nullable = false)
    private Idea idea;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
}