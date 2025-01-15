package com.edison.project.domain.label.dto;

import lombok.*;

public class LabelResponseDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateResultDto {
        private Long labelId;
        private String name;
        private String color;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResultDto {
        private Long labelId;
        private String name;
        private String color;
        private Long bubbleCount;
    }
}
