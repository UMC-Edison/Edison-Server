package com.edison.project.domain.artletter.dto;

import com.edison.project.domain.artletter.entity.ArtletterCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequestDto {

        @NotBlank(message = "title은 비어있을 수 없습니다.")
        @Size(max = 20, message = "title은 최대 20자까지 허용됩니다.")
        private String title;

        @NotBlank(message = "content는 비어있을 수 없습니다.")
        private String content;

        @NotNull(message = "category는 null일 수 없습니다.")
        private ArtletterCategory category;

        @NotBlank(message = "writer는 비어있을 수 없습니다.")
        @Pattern(regexp = "^[a-zA-Z가-힣\\s]+$", message = "writer는 문자만 허용됩니다.")
        private String writer;

        @NotNull(message = "readTime은 0보다 큰 정수여야 합니다.")
        @Min(value = 1, message = "readTime은 0보다 큰 정수여야 합니다.")
        private Integer readTime;

        @Size(max = 50, message = "tag는 최대 50자까지 허용됩니다.")
        private String tag;

        @NotNull(message = "thumbnail은 null일 수 없습니다.")
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
        private String thumbnail;
        private ArtletterCategory category;
        private int readTime;
        private String tag;
        private LocalDateTime createdAt;
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
        private boolean isScraped;
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
        private LocalDateTime ScrapedAt;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class recommendKeywordDto {
        private Long artletterId;
        private String keyword;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendCategoryResponse {
        private List<String> categories;
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

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryKeywordRequestDto {
        private String keyword;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryKeywordResponseDto {
        private List<String> keywords;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryResponseDto {
        private Long artletterId;
        private String title;
        private String thumbnail;
        private String tags;
        private boolean isScraped;
    }

}

