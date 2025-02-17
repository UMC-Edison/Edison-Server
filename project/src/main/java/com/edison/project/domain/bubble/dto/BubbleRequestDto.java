package com.edison.project.domain.bubble.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
public class BubbleRequestDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListDto {
        private String title;
        private String content;
        private String mainImageUrl;
        private Set<Long> labelIdxSet;  // 중복 방지
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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncDto {
        @NotNull(message = "(DTO)버블 ID는 필수입니다.")
        private Long BubbleId;

        private String title;
        private String content;
        private String mainImageUrl;
        private Set<Long> labelIdxs;
        private Set<Long> backlinkIds;

        @NotNull(message = "(DTO)삭제 여부는 필수입니다.")
        @JsonProperty("isDeleted")
        private boolean isDeleted;

        @JsonProperty("isTrashed")
        private boolean isTrashed;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;

        public boolean isDeleted() {
            return isDeleted;
        }
        public boolean isTrashed() {
            return isTrashed;
        }
    }
}
