package com.edison.project.domain.space.dto;

public class SpaceInfoResponseDto {
    private int groupId;
    private double centerX;
    private double centerY;
    private double radius;

    // 기본 생성자 추가
    public SpaceInfoResponseDto() {
    }

    public SpaceInfoResponseDto(int groupId, double centerX, double centerY, double radius) {
        this.groupId = groupId;
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
    }

    public int getGroupId() {
        return groupId;
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