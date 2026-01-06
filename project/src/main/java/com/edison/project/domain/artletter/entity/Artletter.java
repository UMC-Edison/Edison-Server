package com.edison.project.domain.artletter.entity;

import com.edison.project.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "writer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_artletter_writer"))
    private Writer writer;

    @Column(name = "read_time", nullable = false)
    private int readTime;

    @Column(name = "tag", length = 100)
    private String tag;

    @Column(name = "keyword", length = 100)
    private String keyword;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ArtletterCategory category;

    @Column(name = "thumbnail", columnDefinition = "TEXT", nullable = false)
    private String thumbnail;

    public ArtletterCategory getCategory() {
        return this.category;
    }
}
