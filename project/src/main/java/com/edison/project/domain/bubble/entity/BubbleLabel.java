package com.example.project.domain.bubble.entity;

import com.example.project.domain.label.entity.Label;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "BubbleLabel", indexes = {
        @Index(name = "idx_bubble_label_bubble_id", columnList = "bubble_id"),
        @Index(name = "idx_bubble_label_label_id", columnList = "label_id")
})
public class BubbleLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bubble_label_id")
    private Integer bubbleLabelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bubble_id", nullable = false)
    private Bubble bubble;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "label_id", nullable = false)
    private Label label;
}
