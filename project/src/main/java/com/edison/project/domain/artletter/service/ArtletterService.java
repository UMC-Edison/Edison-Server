package com.edison.project.domain.artletter.service;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.domain.artletter.dto.ArtletterDTO;
import com.edison.project.domain.artletter.entity.Artletter;
import com.edison.project.global.security.CustomUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;


public interface ArtletterService {
    Page<Artletter> getAllArtletters(int page, int size);

    ArtletterDTO.CreateResponseDto createArtletter(ArtletterDTO.CreateRequestDto request);

    ArtletterDTO.LikeResponseDto likeToggleArtletter(CustomUserPrincipal userPrincipal, Long letterId);

    ArtletterDTO.ScrapResponseDto scrapToggleArtletter(CustomUserPrincipal userPrincipal, Long letterId);

    Page<Artletter> searchArtletters(String keyword, Pageable pageable);
}
