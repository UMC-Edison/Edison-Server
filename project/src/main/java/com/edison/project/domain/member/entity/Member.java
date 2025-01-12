package com.edison.project.domain.member.entity;

import com.edison.project.domain.label.entity.Label;
import com.edison.project.global.common.entity.BaseEntity;
import com.edison.project.domain.bubble.entity.Bubble;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "Member", indexes = {
        @Index(name = "idx_member_email", columnList = "email"),
        @Index(name = "idx_member_nickname", columnList = "nickname")
})

public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bubble> bubbles = new ArrayList<>();

    @Column(name = "provider_id", length = 100)
    private String providerId; // 구글 로그인 식별 용도

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Label> labels = new ArrayList<>();
}
