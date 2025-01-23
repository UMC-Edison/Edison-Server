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

        @NotNull(message = "라벨 ID는 필수입니다.")
        private Long labelId;

        @NotBlank(message = "라벨 이름은 필수입니다.")
        private String name;

        @Min(value = 0, message = "컬러는 0 이상의 숫자여야 합니다.")
        @Max(value = 999999999, message = "컬러는 최대 10자리 이하의 숫자여야 합니다.")
        private int color;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateDto {

        @NotBlank(message = "라벨 이름은 필수입니다.")
        private String name;

        @Min(value = 0, message = "컬러는 0 이상의 숫자여야 합니다.")
        @Max(value = 999999999, message = "컬러는 최대 10자리 이하의 숫자여야 합니다.")
        private int color;
    }

}
