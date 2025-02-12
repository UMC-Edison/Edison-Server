package com.edison.project.domain.space.dto;

import com.edison.project.domain.bubble.entity.Bubble;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties ({"hibernateLazyInitializer", "handler"})
public class SpaceResponseDto {
    private Long id;
    private String content;
    private double x;
    private double y;
    private Integer group = 0;

    // 올바른 생성자 추가
    public SpaceResponseDto(Bubble bubble, String content, double x, double y, Integer group) {
        this.id = bubble.getBubbleId();
        this.content = content;
        this.x = x;
        this.y = y;
        this.group = group;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public int getGroup() {
        return this.group != null ? this.group : 0;  // ✅ null이면 0 반환
    }

    public void setGroup(int group) {
        this.group = group;
    }
}
