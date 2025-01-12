package com.edison.project.domain.member.entity;

import com.edison.project.domain.common.entity.BaseEntity;
import com.edison.project.domain.bubble.entity.Bubble;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "Member", indexes = {
        @Index(name = "idx_member_email", columnList = "email"),
        @Index(name = "idx_member_nickname", columnList = "nickname")
})
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor

public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bubble> bubbles = new ArrayList<>();

}
