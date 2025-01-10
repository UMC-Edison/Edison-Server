package com.edison.project.domain.images.entity;

import com.edison.project.domain.artletter.entity.Artletter;
import com.edison.project.domain.board.entity.Board;
import com.edison.project.domain.bubble.entity.Bubble;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Images")
public class Images {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "img_id")
    private Long imgId;

    @ManyToOne
    @JoinColumn(name = "bubble_id")
    private Bubble bubble;

    @ManyToOne
    @JoinColumn(name = "board_id")
    private Board board;

    @ManyToOne
    @JoinColumn(name = "letter_id")
    private Artletter artletter;

    @Column(name = "is_main", nullable = false)
    private Boolean isMain = false;

    @Column(name = "img_url", nullable = false, length = 2083)
    private String imgUrl;
}
