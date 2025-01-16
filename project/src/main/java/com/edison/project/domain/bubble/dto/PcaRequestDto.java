package com.edison.project.domain.bubble.dto;

public class PcaRequestDto {

    private double[][] data;
    private int[] labels;

    public double[][] getData() {
        return data;
    }

    public void setData(double[][] data) {
        this.data = data;
    }

    public int[] getLabels() {
        return labels;
    }

    public void setLabels(int[] labels) {
        this.labels = labels;
    }
}
