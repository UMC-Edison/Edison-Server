package com.edison.project.domain.member.entity;

import com.edison.project.domain.label.entity.Label;
import com.edison.project.global.common.entity.BaseEntity;
import com.edison.project.domain.bubble.entity.Bubble;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

public class  Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "profile_img", length = 2083)
    private String profileImg;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(nullable = false)
    private String role;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bubble> bubbles = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Label> labels = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberMemory> memoryMemories = new ArrayList<>();

    public Member(String email) {
        super();
    }

}
