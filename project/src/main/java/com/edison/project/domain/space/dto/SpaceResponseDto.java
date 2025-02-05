package com.edison.project.domain.space.dto;

import com.edison.project.domain.bubble.entity.Bubble;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.ElementCollection;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties ({"hibernateLazyInitializer", "handler"})
public class SpaceResponseDto {
    private Long id;
    private String content;
    private double x;
    private double y;

    @ElementCollection // JPA 리스트 관리용
    private List<String> groups;

    // 올바른 생성자 추가
    public SpaceResponseDto(Bubble bubble, String content, double x, double y, List<String> groups) {
        this.id = bubble.getBubbleId();
        this.content = content;
        this.x = x;
        this.y = y;
        this.groups = new ArrayList<>(groups);;
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

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }
}
