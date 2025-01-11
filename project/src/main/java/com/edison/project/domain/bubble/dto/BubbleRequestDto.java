package com.edison.project.domain.bubble.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
public class BubbleRequestDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateDto {
        private Long memberId;
        private String title;
        private String content;
        private String mainImageUrl;
        private List<Long> labels;
        private Long linkedBubble;
    }
}
