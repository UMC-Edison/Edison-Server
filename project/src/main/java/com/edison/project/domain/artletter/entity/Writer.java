package com.edison.project.domain.artletter.entity;

import com.edison.project.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "writer")
public class Writer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "writer_id")
    private Long writerId;

    @Column(name = "writer_name")
    private String writerName;

    @Column(name = "profile_img")
    private String profileImg;

    @Column(name = "writer_url")
    private String writerUrl;

}
