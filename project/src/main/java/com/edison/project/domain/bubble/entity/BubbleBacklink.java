package com.edison.project.domain.bubble.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Table(name = "bubble_backlink")
public class BubbleBacklink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bubble_id", nullable = false)
    private Bubble bubble;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backlink_bubble_id", nullable = false)
    private Bubble backlinkBubble;

    @Column(name = "is_trashed", nullable = false)  //휴지통에 있는 지(soft_delete)
    private boolean isTrashed = false;

}
