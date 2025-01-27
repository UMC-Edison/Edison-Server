package com.edison.project.domain.member.entity;

import com.edison.project.domain.label.entity.Label;
import com.edison.project.global.common.entity.BaseEntity;
import com.edison.project.domain.bubble.entity.Bubble;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.transaction.annotation.Transactional;

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

    @Column(name = "profile_img", length = 2083)
    private String profileImg;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bubble> bubbles = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Label> labels = new ArrayList<>();

    //복사 빌더 메서드
    public Member registerProfile(String nickname) {
        return Member.builder()
                .memberId(this.memberId)
                .email(this.email)
                .nickname(nickname)
                .profileImg(this.profileImg)
                .isDeleted(this.isDeleted)
                .bubbles(new ArrayList<>(this.bubbles))
                .labels(new ArrayList<>(this.labels))
                .build();
    }

    @Transactional
    public void updateProfile(String nickname, String profileImg) {
        this.nickname = nickname;
        this.profileImg = profileImg;
    }

    @Transactional
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}
