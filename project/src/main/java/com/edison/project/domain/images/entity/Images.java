package com.example.project.domain.images.entity;

import com.example.project.common.entity.BaseEntity;
import com.example.project.domain.artletter.entity.Artletter;
import com.example.project.domain.board.entity.Board;
import com.example.project.domain.bubble.entity.Bubble;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Images")
public class Images extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long imgId;

    @ManyToOne
    @JoinColumn(name = "bubble_id", nullable = false)
    private Bubble bubble;

    @ManyToOne
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne
    @JoinColumn(name = "letter_id", nullable = false)
    private Artletter artletter;

    private Boolean isMain;
    private String imgUrl;
}
