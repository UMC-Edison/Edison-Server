package com.edison.project.domain.label.dto;

import com.edison.project.domain.bubble.dto.BubbleResponseDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class LabelResponseDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelSimpleInfoDto {
        private String localIdx;
        private String name;
        private int color;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResultDto {
        private String localIdx;
        private String name;
        private int color;
        private Long bubbleCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailResultDto {
        private String localIdx;
        private String name;
        private int color;
        private Long bubbleCount;
        private List<BubbleResponseDto.SyncResultDto> bubbles;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelSyncResponseDTO {
        private String localIdx;
        private String name;
        private int color;
        private Boolean isDeleted;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
    }
}
