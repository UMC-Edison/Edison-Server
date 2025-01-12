package com.edison.project.domain.label.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
public class LabelRequestDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateDto {

        // @NotNull(message = "(DTO)유저 ID는 필수입니다.")
        private Long userId;

        @NotBlank(message = "(DTO)라벨 이름은 필수입니다.")
        private String name;

        @NotBlank(message = "(DTO)라벨 색상은 필수입니다.")
        private String color;
    }
}
