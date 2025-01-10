package com.edison.project.domain.member.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "MemberKeyword", indexes = {
        @Index(name = "idx_member_keyword_member_id", columnList = "member_id"),
        @Index(name = "idx_member_keyword_keyword_id", columnList = "keyword_id")
})
public class MemberKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_keyword_id")
    private Long memberKeywordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "keyword_id", nullable = false)
    private Integer keywordId;
}