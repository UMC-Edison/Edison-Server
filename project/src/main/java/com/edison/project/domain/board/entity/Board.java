package com.example.project.domain.board.entity;

import com.example.project.domain.common.entity.BaseEntity;
import com.example.project.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Board")
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boardId;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private String name;
}
