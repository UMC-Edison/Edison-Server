package com.edison.project.domain.artletter.service;

import com.edison.project.domain.artletter.dto.ArtletterDto;
import com.edison.project.domain.artletter.repository.ArtletterLikesRepository;
import com.edison.project.domain.artletter.dto.PageInfoDto;
import com.edison.project.domain.artletter.dto.TestDto;
import com.edison.project.domain.artletter.repository.ArtletterRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.edison.project.domain.artletter.entity.Artletter;

import java.util.List;

@Service
public class ArtletterService {

    private final ArtletterRepository artletterRepository;
    private final ArtletterLikesRepository likesRepository;

    public ArtletterService(ArtletterRepository artletterRepository, ArtletterLikesRepository likesRepository) {
        this.artletterRepository = artletterRepository;
        this.likesRepository = likesRepository;
    }

    public TestDto getAllArtletters(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Artletter> artletters = artletterRepository.findAll(pageable);

        List<ArtletterDto> results = artletters.stream()
                .map(artletter -> {
                    int likes = likesRepository.countByArtletter(artletter);
                    int scraps = 0;
                    return ArtletterDto.fromEntity(artletter, likes, scraps);
                })
                .toList();

        return new TestDto(
                true,
                200,
                "조회 성공",
                new PageInfoDto(
                        artletters.getNumber(),
                        artletters.getSize(),
                        artletters.getTotalElements(),
                        artletters.getTotalPages()
                ),
                results
        );
    }
}
