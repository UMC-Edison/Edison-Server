package com.edison.project.domain.artletter.entity;

import com.edison.project.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    private String thumbnail;

    // Builder 패턴 적용
    @Builder
    public Artletter(Long letterId, String title, String content, String writer, int readTime, String tag, ArtletterCategory category) {
        this.letterId = letterId;
        this.title = title;
        this.content = content;
        this.writer = writer;
        this.readTime = readTime;
        this.tag = tag;
        this.category = category;
        this.thumbnail = thumbnail;
    }
}
