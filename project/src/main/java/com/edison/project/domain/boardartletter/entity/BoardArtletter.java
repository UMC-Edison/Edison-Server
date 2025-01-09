package com.example.project.domain.board.entity;

import com.example.project.domain.artletter.entity.Artletter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "BoardArtletter")
public class BoardArtletter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boardLetterId;

    @ManyToOne
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne
    @JoinColumn(name = "letter_id", nullable = false)
    private Artletter artletter;
}
