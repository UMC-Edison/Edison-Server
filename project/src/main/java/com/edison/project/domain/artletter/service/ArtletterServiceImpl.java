package com.edison.project.domain.artletter.service;

import com.edison.project.domain.artletter.dto.ArtletterDTO;
import com.edison.project.domain.artletter.entity.Artletter;
import com.edison.project.domain.artletter.repository.ArtletterRepository;
import com.edison.project.domain.artletter.service.ArtletterService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArtletterServiceImpl implements ArtletterService {

    private final ArtletterRepository artletterRepository;

    public ArtletterServiceImpl(ArtletterRepository artletterRepository) {
        this.artletterRepository = artletterRepository;
    }

    @Override
    public ArtletterDTO.ListResponseDto getAllArtletters(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Artletter> artletters = artletterRepository.findAll(pageable); // JpaRepository 기본 메서드 사용

        List<ArtletterDTO.CreateResponseDto> results = artletters.getContent().stream()
                .map(artletter -> ArtletterDTO.CreateResponseDto.builder()
                        .id(artletter.getLetterId())
                        .title(artletter.getTitle())
                        .build())
                .collect(Collectors.toList());

        return ArtletterDTO.ListResponseDto.builder()
                .artletters(results)
                .totalPages(artletters.getTotalPages())
                .totalElements(artletters.getTotalElements())
                .build();
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
                .id(savedArtletter.getLetterId())
                .title(savedArtletter.getTitle())
                .build();
    }

    @Override
    public ArtletterDTO.ListResponseDto searchArtletters(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Artletter> artletters = artletterRepository.searchByKeyword(keyword, pageable); // Custom 메서드 사용

        List<ArtletterDTO.CreateResponseDto> results = artletters.getContent().stream()
                .map(artletter -> ArtletterDTO.CreateResponseDto.builder()
                        .id(artletter.getLetterId())
                        .title(artletter.getTitle())
                        .build())
                .collect(Collectors.toList());

        return ArtletterDTO.ListResponseDto.builder()
                .artletters(results)
                .totalPages(artletters.getTotalPages())
                .totalElements(artletters.getTotalElements())
                .build();
    }
}
