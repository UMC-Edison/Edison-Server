package com.edison.project.domain.space.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "spaces")
public class Space {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;
    private double x;
    private double y;

    @ElementCollection
    @CollectionTable(name = "space_groups", joinColumns = @JoinColumn(name = "space_id"))
    @Column(name = "`group_names`")  // ✅ 예약어 문제 해결
    private List<String> groupNames;  // ✅ 필드명 변경

    public Space() {}

    public Space(String content, double x, double y, List<String> groupNames, Long bubbleId) {
        this.content = content;
        this.x = x;
        this.y = y;
        this.groupNames = groupNames;
    }

    // ✅ Getter & Setter 수정
    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public List<String> getGroupNames() { // ✅ 변경된 필드명 반영
        return groupNames;
    }

    public void setGroupNames(List<String> groupNames) { // ✅ Setter도 수정
        this.groupNames = groupNames;
    }
}
