package com.edison.project.domain.artletter.service;

import com.edison.project.domain.artletter.dto.ArtletterDTO;
import com.edison.project.domain.artletter.dto.TestDTO;
import com.edison.project.domain.artletter.entity.Artletter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface ArtletterService {
    TestDTO getAllArtletters(int page, int size);

    ArtletterDTO.CreateResponseDto createArtletter(ArtletterDTO.CreateRequestDto request);

    Page<Artletter> searchArtletters(String keyword, Pageable pageable);
}
