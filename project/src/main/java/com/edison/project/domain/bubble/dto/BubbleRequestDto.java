package com.edison.project.domain.bubble.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
public class BubbleRequestDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListDto {

        private Long memberId;
        private String title;
        private String content;
        private String mainImageUrl;
        private Set<Long> labelIds;  // 중복 방지
        private Long linkedBubbleId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteDto {
        private Long memberId;
        private Long bubbleId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestoreDto {
        private Long memberId;
        private Long bubbleId;;
    }
}
