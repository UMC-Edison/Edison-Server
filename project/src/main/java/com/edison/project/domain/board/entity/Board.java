package com.edison.project.domain.board.entity;

import com.edison.project.domain.common.entity.BaseEntity;
import com.edison.project.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Board", indexes = {
        @Index(name = "idx_board_member_id", columnList = "member_id"),
        @Index(name = "idx_board_name", columnList = "name")
})
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long boardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "name", nullable = false, length = 100)
    private String name;
}
