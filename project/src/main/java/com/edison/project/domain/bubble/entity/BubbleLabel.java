package com.example.project.domain.bubble.entity;

import com.example.project.domain.label.entity.Label;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "BubbleLabel")
public class BubbleLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer bubbleLabelId;

    @ManyToOne
    @JoinColumn(name = "bubble_id", nullable = false)
    private Bubble bubble;

    @ManyToOne
    @JoinColumn(name = "label_id", nullable = false)
    private Label label;
}
