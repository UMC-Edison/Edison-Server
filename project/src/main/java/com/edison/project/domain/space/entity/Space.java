package com.edison.project.domain.space.entity;

import java.util.List;

public class Space {
    private String content; // Space 내용
    private double x;       // x 좌표
    private double y;       // y 좌표
    private List<String> groups; // 속한 그룹

    public Space(String content, double x, double y, List<String> groups) {
        this.content = content;
        this.x = x;
        this.y = y;
        this.groups = groups;
    }

    // Getter와 Setter
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
