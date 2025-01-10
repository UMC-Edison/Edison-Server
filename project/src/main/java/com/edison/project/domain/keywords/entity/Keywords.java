package com.example.project.domain.keywords.entity;

import com.example.project.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Keywords", indexes = {
    @Index(name = "idx_keywords_name", columnList = "name"),
    @Index(name = "idx_keywords_category", columnList = "category")
})
public class Keywords extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keyword_id")
    private Integer keywordId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "category", length = 50)
    private String category;
}
