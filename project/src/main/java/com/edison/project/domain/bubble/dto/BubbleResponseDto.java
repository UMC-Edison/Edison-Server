package com.edison.project.domain.bubble.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class BubbleResponseDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateResultDto {
        private Long bubbleId;
        private String title;
        private String content;
        private String mainImageUrl;
        private Set<Long> labels;
        private Long linkedBubbleId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteResultDto {
        private Long bubbleId;
        private boolean isDeleted;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestoreResultDto {
        private Long bubbleId;
        private boolean isRestored;
    }
}
