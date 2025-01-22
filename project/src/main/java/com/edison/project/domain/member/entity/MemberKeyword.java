package com.edison.project.domain.member.entity;

import com.edison.project.domain.keywords.entity.Keywords;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "MemberKeyword", indexes = {
        @Index(name = "idx_member_keyword_member_id", columnList = "member_id"),
        @Index(name = "idx_member_keyword_keyword_id", columnList = "keyword_id")
})

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_keyword_id")
    private Long memberKeywordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keywords keyword;
}