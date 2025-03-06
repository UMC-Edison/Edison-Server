package com.edison.project.domain.artletter.service;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.domain.artletter.dto.ArtletterDto;
import com.edison.project.domain.artletter.entity.ArtletterCategory;
import com.edison.project.global.security.CustomUserPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;


public interface ArtletterService {
    ResponseEntity<ApiResponse> getAllArtlettersResponse(CustomUserPrincipal userPrincipal, int page, int size, String sortType);
    ResponseEntity<ApiResponse> searchArtletters(CustomUserPrincipal userPrincipal, String keyword, int page, int size, String sortType);
    ArtletterDto.ListResponseDto getArtletter(CustomUserPrincipal userPrincipal, long letterId);

    ResponseEntity<ApiResponse> getMemoryKeyword(CustomUserPrincipal userPrincipal);
    ResponseEntity<ApiResponse> deleteMemoryKeyword(CustomUserPrincipal userPrincipal, String keyword);

    ArtletterDto.LikeResponseDto likeToggleArtletter(CustomUserPrincipal userPrincipal, Long letterId);
    ArtletterDto.ScrapResponseDto scrapToggleArtletter(CustomUserPrincipal userPrincipal, Long letterId);

    ResponseEntity<ApiResponse> getEditorArtletters(CustomUserPrincipal userPrincipal, ArtletterDto.EditorRequestDto editorRequestDto);
    List<ArtletterDto.recommendKeywordDto> getRecommendKeyword(List<Long> artletterIds);
    List<String> getRecommendCategory();

    ResponseEntity<ApiResponse> getScrapArtlettersByCategory(CustomUserPrincipal userPrincipal, Pageable pageable);
    ResponseEntity<ApiResponse> getScrapCategoryArtletters(CustomUserPrincipal userPrincipal, ArtletterCategory category, Pageable pageable);

    ArtletterDto.CreateResponseDto createArtletter(CustomUserPrincipal userPrincipal, ArtletterDto.CreateRequestDto request);
    ResponseEntity<ApiResponse> getArtlettersByCategory(CustomUserPrincipal userPrincipal, ArtletterCategory category, Pageable pageable);
}
