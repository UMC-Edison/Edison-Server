package com.edison.project.domain.space.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Dataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "category")
    private String category;

    public Dataset(String content, String category) {
        this.content = content;
        this.category = category;
    }
}
