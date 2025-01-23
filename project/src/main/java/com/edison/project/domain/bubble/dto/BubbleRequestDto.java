package com.edison.project.domain.bubble.dto;

import jakarta.validation.constraints.NotNull;
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
    public static class CreateDto {
        @NotNull(message = "버블 ID는 필수입니다.")
        private Long bubbleId;
        private String title;
        private String content;
        private String mainImageUrl;
        private Set<Long> labelIds;
        private Long linkedBubbleId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListDto {
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
        private Long bubbleId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestoreDto {
        private Long bubbleId;
    }
}
