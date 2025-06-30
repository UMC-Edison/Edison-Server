package com.edison.project.domain.space.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceMapRequestDto {

    private List<MapRequestDto> memos;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MapRequestDto {
        private String localIdx;
        private String content;
    }
}
