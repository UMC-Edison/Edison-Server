package com.edison.project.domain.label.entity;

import com.edison.project.domain.member.entity.Member;
import com.edison.project.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Label extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "label_id")
    private Long labelId;

    @Column(name = "name")
    private String name;

    @Column(name = "color", nullable = false, length = 10)
    private String color;

    @ManyToOne
    @JoinColumn(name = "member_id") // 로그인 안한 유저 id값 없다면, nullable=true 추가
    private Member member;

}
