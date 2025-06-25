package com.edison.project.domain.label.entity;

import com.edison.project.domain.bubble.entity.BubbleLabel;
import com.edison.project.domain.member.entity.Member;
import com.edison.project.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "Label", indexes = {
        @Index(name = "idx_localIdx", columnList = "local_idx")})
public class Label {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "label_id")
    private Long labelId;

    @Column(name="local_idx", length = 50)
    private String localIdx;

    @Column(name = "name")
    private String name;

    @Column(name = "color", nullable = false)
    private int color;

    @Column(name = "created_at", nullable = false, updatable = false) // 생성 시점 변경 방지
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    // BubbleLabel과 연관관계 설정 -> Label 삭제 시 연관된 BubbleLabel도 삭제
    @OneToMany(mappedBy = "label", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<BubbleLabel> bubbleLabels = new HashSet<>();

    // 생성자 및 빌더 추가
    @Builder
    public Label(String labelId, String name, int color, Member member, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.localIdx = labelId;
        this.name = name;
        this.color = color;
        this.member = member;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now(); // 기본값 설정 가능
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
        this.deletedAt = deletedAt;
    }

}
