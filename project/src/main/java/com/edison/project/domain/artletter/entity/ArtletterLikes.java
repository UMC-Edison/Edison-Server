package com.example.project.domain.artletter.entity;

import com.example.project.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ArtletterLikes", uniqueConstraints = {
        @UniqueConstraint(name = "uq_artletter_member", columnNames = {"artletter_id", "member_id"})
}, indexes = {
        @Index(name = "idx_artletter_likes_artletter_id", columnList = "artletter_id"),
        @Index(name = "idx_artletter_likes_member_id", columnList = "member_id")
})
public class ArtletterLikes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "artletter_likes_id")
    private Long artletterLikesId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artletter_id", nullable = false)
    private Artletter artletter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}
