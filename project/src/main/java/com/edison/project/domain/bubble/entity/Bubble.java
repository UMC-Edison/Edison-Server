package com.example.project.domain.bubble.entity;

import com.example.project.domain.common.entity.BaseEntity;
import com.example.project.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Bubble")
public class Bubble extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bubbleId;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private String title;
    private String content;
    private String mainImg;
    private boolean isDeleted;

    @ManyToOne
    @JoinColumn(name = "linked_bubble")
    private Bubble linkedBubble;

}


