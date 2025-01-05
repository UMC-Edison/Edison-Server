package com.example.project.domain.artletter.entity;

import com.example.project.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Artletter")
public class Artletter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long letterId;

    private String title;
    private String content;
    private String tag;

    @Enumerated(EnumType.STRING)
    private ArtletterCategory category;

    public enum ArtletterCategory {
        CATEGORY1, CATEGORY2, CATEGORY3
    }
}
