package com.edison.project.domain.keywords.service;

import com.edison.project.domain.member.dto.MemberResponseDto;

public interface KeywordsService {
    MemberResponseDto.IdentityKeywordsResultDto getKeywordsByCategory(String category);
}
