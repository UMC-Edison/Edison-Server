package com.edison.project.domain.artletter.service;

import com.edison.project.domain.artletter.dto.ArtletterDTO;

public interface ArtletterService {
    ArtletterDTO.ListResponseDto getAllArtletters(int page, int size);

    ArtletterDTO.CreateResponseDto createArtletter(ArtletterDTO.CreateRequestDto request);

    ArtletterDTO.ListResponseDto searchArtletters(String keyword, int page, int size);
}
