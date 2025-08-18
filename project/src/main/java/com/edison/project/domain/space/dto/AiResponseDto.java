package com.edison.project.domain.space.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class AiResponseDto {

    @Getter
    @NoArgsConstructor
    public static class AiVectorResponseDto {
        private String localIdx;
        private Double x;
        private Double y;
    }

    @Getter
    @NoArgsConstructor
    public static class AiSimilarityResponseDto {
        private String keyword;
        private List<AiSimilarityResultItem> results;
    }

    @Getter
    @NoArgsConstructor
    public static class AiSimilarityResultItem {
        private String localIdx;
        private Double similarity;
    }
}