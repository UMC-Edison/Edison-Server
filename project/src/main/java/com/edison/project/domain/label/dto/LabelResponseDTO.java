package com.edison.project.domain.label.dto;

import com.edison.project.domain.bubble.dto.BubbleResponseDto;
import lombok.*;

import java.util.List;

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

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailResultDto {
        private Long labelId;
        private String name;
        private String color;
        private Long bubbleCount;
        private List<BubbleResponseDto.ListResultDto> bubbles;
    }
}
