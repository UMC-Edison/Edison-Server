package com.edison.project.domain.artletter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.edison.project.domain.artletter.entity.Artletter;

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
        private Artletter.ArtletterCategory category;
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
    public static class CreateResponseDto {
        private Long artletterId;
        private String title;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ListResponseDto {
        private Long artletterId;
        private String title;
        private String thumbnail;
        private int likes;
        private int scraps;
    }

    public static ArtletterDTO fromEntity(Artletter artletter, int likes, int scraps) {
        return new ArtletterDTO(
                artletter.getLetterId(),
                artletter.getTitle(),
                "thumbnail_url_placeholder",
                // 아트레터 내부에 사진 들어가는건 아직 구현 안 됨
                // 내부에 최대 5장, 썸네일 1장
                likes,
                scraps
        );
    }


}

