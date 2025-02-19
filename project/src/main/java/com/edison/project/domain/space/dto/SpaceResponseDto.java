package com.edison.project.domain.space.dto;

import com.edison.project.domain.bubble.entity.Bubble;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties ({"hibernateLazyInitializer", "handler"})
public class SpaceResponseDto {
    private Long id;
    private double x;
    private double y;

    // 올바른 생성자 추가
    public SpaceResponseDto(Bubble bubble, double x, double y) {
        this.id = bubble.getLocalIdx();
        this.x = x;
        this.y = y;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

}
