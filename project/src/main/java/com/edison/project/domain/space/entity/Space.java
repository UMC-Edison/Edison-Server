package com.edison.project.domain.space.entity;

import com.edison.project.domain.bubble.entity.Bubble;
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

    // ✅ Bubble과의 관계 설정 (ManyToOne)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bubble_id", nullable = false) // 🚨 `NOT NULL` 적용
    private Bubble bubble;

    @ElementCollection
    @CollectionTable(name = "space_groups", joinColumns = @JoinColumn(name = "space_id"))
    @Column(name = "group_names")  // ✅ 예약어 문제 해결 (`groups` → `group_names`)
    private List<String> groupNames;

    public Space() {}

    public Space(String content, double x, double y, List<String> groupNames, Bubble bubble) {
        this.content = content;
        this.x = x;
        this.y = y;
        this.groupNames = groupNames;
        this.bubble = bubble; // ✅ `bubble_id` 설정
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

    public Bubble getBubble() { // ✅ Bubble 관련 Getter 추가
        return bubble;
    }

    public void setBubble(Bubble bubble) { // ✅ Bubble 관련 Setter 추가
        this.bubble = bubble;
    }

    public List<String> getGroupNames() { // ✅ 변경된 필드명 반영
        return groupNames;
    }

    public void setGroupNames(List<String> groupNames) { // ✅ Setter도 수정
        this.groupNames = groupNames;
    }
}
