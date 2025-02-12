package com.edison.project.domain.space.dto;

public class SpaceInfoResponseDto {
    private double centerX;
    private double centerY;
    private double radius;

    // 기본 생성자 추가
    public SpaceInfoResponseDto() {
    }

    public SpaceInfoResponseDto(double centerX, double centerY, double radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public double getRadius() {
        return radius;
    }
}
