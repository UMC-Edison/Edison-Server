package com.edison.project.domain.artletter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.edison.project.domain.artletter.entity.Artletter;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArtletterDto {
    private Long artletterId;
    private String title;
    private String thumbnail;
    private int likes;
    private int scraps;

    public static ArtletterDto fromEntity(Artletter artletter, int likes, int scraps) {
        return new ArtletterDto(
                artletter.getLetterId(),
                artletter.getTitle(),
                "thumbnail_url_placeholder",
                likes,
                scraps
        );
    }
}

