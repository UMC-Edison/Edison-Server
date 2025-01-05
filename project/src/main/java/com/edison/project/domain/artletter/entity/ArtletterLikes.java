package com.example.project.domain.artletter.entity;

import com.example.project.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ArtletterLikes")
public class ArtletterLikes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long artletterLikesId;

    @ManyToOne
    @JoinColumn(name = "artletter_id", nullable = false)
    private Artletter artletter;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}
