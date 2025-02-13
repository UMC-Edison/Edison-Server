package com.edison.project.domain.artletter.dto;

import com.edison.project.domain.artletter.entity.ArtletterCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import com.edison.project.domain.artletter.entity.Artletter;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ArtletterDTO {
    private Long artletterId;
    private String title;
    private String thumbnail;
    private int likes;
    private int scraps;

    @Data
    public static class CreateRequestDto {
        @NotBlank
        private String title;
        @NotBlank
        private String content;
        @NotNull
        private ArtletterCategory category;
        @NotBlank
        private String writer;
        @NotNull
        private Integer readTime;
        private String tag;
        private String thumbnail;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EditorRequestDto {
        private List<Long> artletterIds;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateResponseDto {
        private Long artletterId;
        private String title;
        private int likes;
        private int scraps;
        private boolean isScrap;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ListResponseDto {
        private Long artletterId;
        private String title;
        private String content;
        private ArtletterCategory category;
        private int readTime;
        private String writer;
        private String tags;
        private String thumbnail;
        private int likesCnt;
        private int scrapsCnt;
        private boolean isLiked;
        private boolean isScraped;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LikeResponseDto {
        private Long artletterId;
        private int likesCnt;
        private boolean isLiked;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScrapResponseDto {
        private Long artletterId;
        private int scrapsCnt;
        private boolean isScrapped;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MyScrapResponseDto {
        private Long artletterId;
        private String title;
        private String thumbnail;
        private int likesCnt;
        private int scrapsCnt;
        private LocalDateTime scrappedAt;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class recommendKeywordDto {
        private Long artletterId;
        private String keyword;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class recommendCategoryDto {
        private Long artletterId;
        private ArtletterCategory category;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupedScrapResponseDto {
        private String category;
        private List<MyScrapResponseDto> artletters;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SimpleArtletterResponseDto {
        private Long artletterId;
        private String title;
        private String thumbnail;
        private int likesCnt;
        private int scrapsCnt;
        private boolean isLiked;
        private boolean isScraped;
        private LocalDateTime updatedAt;
    }

}

