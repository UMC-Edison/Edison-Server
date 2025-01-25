package com.edison.project.domain.label.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
public class LabelRequestDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAndUpdateDto {

        @NotBlank(message = "(DTO)라벨 이름은 필수입니다.")
        private String name;

        @Min(value = 0, message = "컬러는 0 이상의 숫자여야 합니다.")
        @Max(value = 999999999, message = "컬러는 최대 10자리 이하의 숫자여야 합니다.")
        private int color;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelSyncRequestDTO {

        @NotNull(message = "(DTO)라벨 ID는 필수입니다.")
        private Long labelId;

        @NotBlank(message = "(DTO)라벨 이름은 필수입니다.")
        private String name;

        private int color;

        @NotNull(message = "(DTO)삭제 여부는 필수입니다.")
        private Boolean isDeleted;

        @NotNull(message = "(DTO)생성 시점은 필수입니다.")
        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;

        private LocalDateTime deletedAt;
    }

}
