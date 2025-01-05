package com.example.project.domain.keywords.entity;

import com.example.project.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Keywords")
public class Keywords extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer keywordId;

    private String name;
    private String category;
}
