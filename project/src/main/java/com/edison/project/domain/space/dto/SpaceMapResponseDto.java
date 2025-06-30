package com.edison.project.domain.space.dto;

import com.edison.project.domain.label.dto.LabelResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class SpaceMapResponseDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MapResponseDto {
        private String localIdx;
        private double x;
        private double y;
    }
}
