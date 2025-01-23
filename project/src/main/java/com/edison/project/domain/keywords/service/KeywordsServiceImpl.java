package com.edison.project.domain.keywords.service;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.domain.keywords.entity.Keywords;
import com.edison.project.domain.keywords.repository.KeywordsRepository;
import com.edison.project.domain.member.dto.MemberResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KeywordsServiceImpl implements KeywordsService {

    private final KeywordsRepository keywordsRepository;

    @Override
    public MemberResponseDto.IdentityKeywordsResultDto getKeywordsByCategory(String category) {

        // 데이터베이스에서 해당 카테고리가 존재하는지 확인
        boolean categoryExists = keywordsRepository.existsByCategory(category);
        if (!categoryExists) {
            throw new GeneralException(ErrorStatus.INVALID_CATEGORY); // 존재하지 않는 카테고리 에러 반환
        }

        // 카테고리에 해당하는 키워드 가져오기
        List<Keywords> keywords = keywordsRepository.findAllByCategory(category);

        // DTO로 변환
        Map<String, List<MemberResponseDto.IdentityKeywordDto>> categoryKeywords = Map.of(
                category,
                keywords.stream()
                        .map(keyword -> MemberResponseDto.IdentityKeywordDto.builder()
                                .keywordId(keyword.getKeywordId())
                                .keywordName(keyword.getName())
                                .build())
                        .collect(Collectors.toList())
        );

        return MemberResponseDto.IdentityKeywordsResultDto.builder()
                .categories(categoryKeywords)
                .build();
    }

}

