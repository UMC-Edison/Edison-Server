package com.edison.project.domain.bubble.entity;

import com.edison.project.global.common.entity.BaseEntity;
import com.edison.project.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Setter
@Table(name = "Bubble", indexes = {
        @Index(name = "idx_bubble_member_id", columnList = "member_id"),
        @Index(name = "idx_bubble_title", columnList = "title")})

public class Bubble extends BaseEntity {

    @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bubble_id")
    private Long bubbleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "main_img", length = 2083)
    private String mainImg;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_bubble")
    private Bubble linkedBubble;

    @OneToMany(mappedBy = "bubble", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<BubbleLabel> labels = new HashSet<>();

    @Builder
    public Bubble(Member member, Long bubbleId, String title, String content, String mainImg, Bubble linkedBubble, Set<BubbleLabel> labels,
                  boolean isDeleted, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.bubbleId = bubbleId;
        this.member = member;
        this.title = title;
        this.content = content;
        this.mainImg = mainImg;
        this.linkedBubble = linkedBubble;
        // 기존 라벨 초기화 후 새로운 라벨 추가
        this.labels.clear();
        this.labels.addAll(labels);
        this.isDeleted = isDeleted;
        this.setCreatedAt(createdAt);
        this.setUpdatedAt(updatedAt);
        this.setDeletedAt(deletedAt);
    }

    public void update(String title, String content, String mainImg, Bubble linkedBubble, Set<BubbleLabel> bubbleLabels) {
        this.title = title;
        this.content = content;
        this.mainImg = mainImg;
        this.linkedBubble = linkedBubble;

        // 기존 라벨 초기화 후 새로운 라벨 추가
        this.labels.clear();
        this.labels.addAll(bubbleLabels);
    }

    public void setLabels(Set<BubbleLabel> labels) {
        this.labels = labels;
    }

    public void setDeleted(boolean deleted) {
        this.isDeleted = deleted;
    }

}


