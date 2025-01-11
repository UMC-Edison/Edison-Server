package com.edison.project.domain.artletter.service;

import com.edison.project.domain.artletter.dto.ArtletterDTO;
import com.edison.project.domain.artletter.entity.Artletter;
import com.edison.project.domain.artletter.repository.ArtletterLikesRepository;
import com.edison.project.domain.artletter.repository.ArtletterRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArtletterServiceImpl implements ArtletterService {

    private final ArtletterRepository artletterRepository;
    private final ArtletterLikesRepository artletterLikesRepository;

    public ArtletterServiceImpl(ArtletterRepository artletterRepository, ArtletterLikesRepository likesRepository) {
        this.artletterRepository = artletterRepository;
        this.artletterLikesRepository = likesRepository;
    }

    @Override
    public ArtletterDTO.ListResponseDto getAllArtletters(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Artletter> artletters = artletterRepository.findAll(pageable);

        List<ArtletterDTO.CreateResponseDto> results = artletters.stream()
                .map(artletter -> ArtletterDTO.CreateResponseDto.builder()
                        .id(artletter.getLetterId())
                        .title(artletter.getTitle())
                        .build())
                .toList();

        return new ArtletterDTO.ListResponseDto(
                results,
                artletters.getTotalPages(),
                artletters.getTotalElements()
        );
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

        Artletter savedArtletter = artletterRepository.save(artletter);

        return ArtletterDTO.CreateResponseDto.builder()
                .id(savedArtletter.getLetterId())
                .title(savedArtletter.getTitle())
                .build();
    }

}
