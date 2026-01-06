package com.edison.project.domain.space.dto;

import com.edison.project.domain.label.dto.LabelResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class SpaceSimilarityRequestDto {
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MapRequestDto {
        private String keyword;
        private List<SpaceMapRequestDto.MapRequestDto> memos;
    }
}
