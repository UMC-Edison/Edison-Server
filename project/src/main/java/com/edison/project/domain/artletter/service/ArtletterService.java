package com.edison.project.domain.artletter.service;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.domain.artletter.dto.ArtletterDTO;
import com.edison.project.domain.artletter.entity.ArtletterCategory;
import com.edison.project.global.security.CustomUserPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;


public interface ArtletterService {
    ResponseEntity<ApiResponse> getAllArtlettersResponse(int page, int size, String sortType);

    ArtletterDTO.CreateResponseDto createArtletter(CustomUserPrincipal userPrincipal, ArtletterDTO.CreateRequestDto request);


    ArtletterDTO.LikeResponseDto likeToggleArtletter(CustomUserPrincipal userPrincipal, Long letterId);

    ArtletterDTO.ScrapResponseDto scrapToggleArtletter(CustomUserPrincipal userPrincipal, Long letterId);

    ResponseEntity<ApiResponse> searchArtletters(String keyword, int page, int size, String sortType);

    ArtletterDTO.ListResponseDto getArtletter(CustomUserPrincipal userPrincipal, long letterId);

    ResponseEntity<ApiResponse> getEditorArtletters(CustomUserPrincipal userPrincipal, ArtletterDTO.EditorRequestDto editorRequestDto);

    ResponseEntity<ApiResponse> getScrapArtletter(CustomUserPrincipal userPrincipal, Pageable pageable);

    List<ArtletterDTO.recommendKeywordDto> getRecommendKeyword(List<Long> artletterIds);

    List<ArtletterDTO.recommendCategoryDto> getRecommendCategory(List<Long> artletterIds);

    ResponseEntity<ApiResponse> getScrapArtlettersByCategory(CustomUserPrincipal userPrincipal, Pageable pageable);

    ResponseEntity<ApiResponse> getScrapCategoryArtletters(CustomUserPrincipal userPrincipal, ArtletterCategory category, Pageable pageable);

}
