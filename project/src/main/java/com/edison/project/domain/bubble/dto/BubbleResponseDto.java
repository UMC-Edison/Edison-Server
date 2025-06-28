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
    public static class TrashedListResultDto {
        private String localIdx;
        private String title;
        private String content;
        private String mainImageUrl;
        private List<LabelResponseDTO.LabelSimpleInfoDto> labels;
        private Set<String> backlinkIdxs;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
        private Integer remainDay;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncResultDto {
        private String localIdx;
        private String title;
        private String content;
        private String mainImageUrl;
        private List<LabelResponseDTO.LabelSimpleInfoDto> labels;
        private Set<String> backlinkIdxs;
        private Boolean isDeleted;
        private Boolean isTrashed;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateResultDto {
        private String localIdx;
        private String title;
        private String content;
        private String mainImageUrl;
        private List<LabelResponseDTO.LabelSimpleInfoDto> labels;
        private Set<String> backlinkIdxs;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteRestoreResultDto {
        private String localIdx;
        private boolean isTrashed;
    }

}
