package com.edison.project.domain.artletter.entity;

import com.edison.project.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Artletter", indexes = {
        @Index(name = "idx_artletter_title", columnList = "title"),
        @Index(name = "idx_artletter_writer", columnList = "writer")
})
public class Artletter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "letter_id")
    private Long letterId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "writer", nullable = false, length = 100)
    private String writer;

    @Column(name = "read_time", nullable = false)
    private int readTime;

    @Column(name = "tag", length = 100)
    private String tag;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ArtletterCategory category;

    public enum ArtletterCategory {
        CATEGORY1, CATEGORY2, CATEGORY3
    }
}
