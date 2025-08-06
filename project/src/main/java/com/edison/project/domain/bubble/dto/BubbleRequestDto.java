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
    public static class SyncDto {
        @NotNull(message = "(DTO)버블 ID는 필수입니다.")
        private String localIdx;

        private String title;
        private String content;
        private String mainImageUrl;
        private Set<String> labelIdxs;
        private Set<String> backlinkIds;

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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateDto {

        private String localIdx;
        private String title;
        private String content;
        private String mainImageUrl;
        private Set<String> labelIdxs;
        private Set<String> backlinkIds;
    }
}
