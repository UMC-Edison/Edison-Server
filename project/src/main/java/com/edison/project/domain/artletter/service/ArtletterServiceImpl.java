package com.edison.project.domain.artletter.service;

import com.edison.project.common.response.PageInfo;
import com.edison.project.domain.artletter.dto.ArtletterDTO;
import com.edison.project.domain.artletter.entity.Artletter;
import com.edison.project.domain.artletter.repository.ArtletterRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ArtletterServiceImpl implements ArtletterService {

    private final ArtletterRepository artletterRepository;

    public ArtletterServiceImpl(ArtletterRepository artletterRepository) {
        this.artletterRepository = artletterRepository;
    }

    public Page<Artletter> getAllArtletters(int page, int size) {
        // 페이지 요청 생성
        Pageable pageable = PageRequest.of(page, size);

        return artletterRepository.findAll(pageable);
    }


    @Override
    public ArtletterDTO.CreateResponseDto createArtletter(ArtletterDTO.CreateRequestDto request) {
        Artletter artletter = Artletter.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .writer(request.getWriter())
                .readTime(request.getReadTime())
                .tag(request.getTag())
                .category(request.getCategory())
                .build();

        Artletter savedArtletter = artletterRepository.save(artletter); // JpaRepository 기본 메서드 사용

        return ArtletterDTO.CreateResponseDto.builder()
                .artletterId(savedArtletter.getLetterId())
                .title(savedArtletter.getTitle())
                .build();
    }

    @Override
    public Page<Artletter> searchArtletters(String keyword, Pageable pageable) {
        return artletterRepository.searchByKeyword(keyword, pageable);
    }
}
