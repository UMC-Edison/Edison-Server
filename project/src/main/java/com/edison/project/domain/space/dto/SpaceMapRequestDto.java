package com.edison.project.domain.space.dto;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceMapRequestDto {

    private List<MapRequestDto> memos;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MapRequestDto {
        private String id;
        private String content;
    }
}
