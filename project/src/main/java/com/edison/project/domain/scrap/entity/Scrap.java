package com.edison.project.domain.scrap.entity;

import com.edison.project.domain.artletter.entity.Artletter;
import com.edison.project.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "Scrap")
public class Scrap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scrap_id")
    private Long scrapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "letter_id", nullable = false)
    private Artletter artletter;

    @Builder
    public Scrap(Member member, Artletter artletter) {
        this.member = member;
        this.artletter = artletter;
    }
}
