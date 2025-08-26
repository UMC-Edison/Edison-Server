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

    // Bubble과의 관계 설정 (ManyToOne)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bubble_id", nullable = false) // `NOT NULL` 적용
    private Bubble bubble;

    @Column(nullable = false) // member_id 추가
    private Long memberId;

    // 기본 생성자
    public Space() {}

    // memberId와 Bubble 포함한 생성자
    public Space(String content, double x, double y, Bubble bubble, Long memberId) {
        this.content = content;
        this.x = x;
        this.y = y;
        this.bubble = bubble;
        this.memberId = memberId;
    }

    // Getter & Setter
    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
}
