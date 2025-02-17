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
        private Long localIdx;
        private String title;
        private String content;
        private String mainImageUrl;
        private List<LabelResponseDTO.LabelSimpleInfoDto> labels;
        private Set<Long> backlinkIds;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Integer remainDay;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncResultDto {
        private Long localIdx;
        private String title;
        private String content;
        private String mainImageUrl;
        private List<LabelResponseDTO.LabelSimpleInfoDto> labels;
        private Set<Long> backlinkIds;
        private Boolean isDeleted;
        private Boolean isTrashed;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
    }

}
