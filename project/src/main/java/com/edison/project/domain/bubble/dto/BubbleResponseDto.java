package com.edison.project.domain.bubble.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

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
        private List<Long> labels;
        private Long likedBubble;
        private LocalDateTime createAt;
        private LocalDateTime updateAt;
    }
}
