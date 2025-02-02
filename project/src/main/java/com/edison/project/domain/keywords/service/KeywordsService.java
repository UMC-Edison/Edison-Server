package com.edison.project.domain.keywords.service;

import com.edison.project.domain.keywords.dto.KeywordsResponseDto;

import java.util.List;

public interface KeywordsService {
    List<KeywordsResponseDto.IdentityKeywordDto> getKeywordsByCategory(String category);
}
