package com.edison.project.domain.artletter.service;

import com.edison.project.common.response.Response;
import com.edison.project.domain.artletter.dto.ArtletterDTO;
import com.edison.project.domain.artletter.entity.ArtletterCategory;
import com.edison.project.global.security.CustomUserPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;


public interface ArtletterService {
    ResponseEntity<Response> getAllArtlettersResponse(CustomUserPrincipal userPrincipal, int page, int size, String sortType);
    ResponseEntity<Response> searchArtletters(CustomUserPrincipal userPrincipal, String keyword, int page, int size, String sortType);
    ArtletterDTO.ListResponseDto getArtletter(CustomUserPrincipal userPrincipal, long letterId);

    ResponseEntity<Response> getMemoryKeyword(CustomUserPrincipal userPrincipal);
    ResponseEntity<Response> deleteMemoryKeyword(CustomUserPrincipal userPrincipal, String keyword);

    ArtletterDTO.LikeResponseDto likeToggleArtletter(CustomUserPrincipal userPrincipal, Long letterId);
    ArtletterDTO.ScrapResponseDto scrapToggleArtletter(CustomUserPrincipal userPrincipal, Long letterId);

    List<ArtletterDTO.ListResponseDto> getEditorArtletters(CustomUserPrincipal userPrincipal);
  
    List<ArtletterDTO.recommendKeywordDto> getRecommendKeyword(List<Long> artletterIds);
    List<String> getRecommendCategory();

    ResponseEntity<Response> getScrapArtletters(CustomUserPrincipal userPrincipal, Pageable pageable);
    ResponseEntity<Response> getScrapCategoryArtletters(CustomUserPrincipal userPrincipal, ArtletterCategory category, Pageable pageable);

    ArtletterDTO.CreateResponseDto createArtletter(CustomUserPrincipal userPrincipal, ArtletterDTO.CreateRequestDto request);
    ResponseEntity<Response> getArtlettersByCategory(CustomUserPrincipal userPrincipal, ArtletterCategory category, Pageable pageable);

    List<ArtletterDTO.CategoryResponseDto> getOtherArtletters(CustomUserPrincipal userPrincipal, Long currentId);
}
