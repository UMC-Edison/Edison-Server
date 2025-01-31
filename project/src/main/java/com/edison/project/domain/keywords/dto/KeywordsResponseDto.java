package com.edison.project.domain.keywords.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class KeywordsResponseDto {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdentityKeywordsResultDto {
        private List<IdentityKeywordDto> result;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdentityKeywordDto {
        private Integer keywordId;
        private String keywordName;
    }
}
