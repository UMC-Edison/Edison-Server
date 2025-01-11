package com.edison.project.domain.label.dto;

import lombok.*;

public class LabelResponseDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateResultDto {
        private Long id;
        private String name;
        private String color;
    }
}
