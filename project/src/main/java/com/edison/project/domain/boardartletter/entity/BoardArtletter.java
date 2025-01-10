package com.edison.project.domain.board.entity;

import com.edison.project.domain.artletter.entity.Artletter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "BoardArtletter", indexes = {
        @Index(name = "idx_board_artletter_board_id", columnList = "board_id"),
        @Index(name = "idx_board_artletter_letter_id", columnList = "letter_id")
})
public class BoardArtletter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_letter_id")
    private Long boardLetterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "letter_id", nullable = false)
    private Artletter artletter;
}
