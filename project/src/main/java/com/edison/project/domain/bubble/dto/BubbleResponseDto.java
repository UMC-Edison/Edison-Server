package com.edison.project.domain.bubble.dto;

import com.edison.project.domain.label.dto.LabelResponseDTO;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class BubbleResponseDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrashResultDto {
        private Long bubbleId;
        private boolean isTrashed;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestoreResultDto {
        private Long bubbleId;
        private boolean isRestored;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrashedListResultDto {
        private Long bubbleId;
        private String title;
        private String content;
        private String mainImageUrl;
        private List<LabelResponseDTO.CreateResultDto> labels;
        private Long linkedBubbleId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Integer remainDay;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncResultDto {
        private Long bubbleId;
        private String title;
        private String content;
        private String mainImageUrl;
        private List<LabelResponseDTO.CreateResultDto> labels;
        private Long linkedBubbleId;
        private Set<Long> backlinkIds;
        private Boolean isDeleted;
        private Boolean isTrashed;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
    }
}
