package com.edison.project.domain.label.dto;

import lombok.*;

public class LabelRequestDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateDto {
        private String name;
        private String color;
    }
}
