package com.edison.project.domain.keywords.service;

import com.edison.project.domain.keywords.dto.KeywordsResponseDto;

public interface KeywordsService {
    KeywordsResponseDto.IdentityKeywordsResultDto getKeywordsByCategory(String category);
}
