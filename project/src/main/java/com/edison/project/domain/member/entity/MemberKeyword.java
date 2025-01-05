package com.example.project.domain.member.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "MemberKeyword")
public class MemberKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberKeywordId;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private Integer keywordId;
}
