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

    @Override
    public Map<String, Object> getAllArtletters(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Artletter> artletterPage = artletterRepository.findAll(pageable);

        // Artletter -> ArtletterDTO.ListResponseDto 변환
        List<ArtletterDTO.ListResponseDto> result = artletterPage.getContent().stream()
                .map(artletter -> ArtletterDTO.ListResponseDto.builder()
                        .artletterId(artletter.getLetterId())
                        .title(artletter.getTitle())
                        .thumbnail("https://example.com/thumbnail" + artletter.getLetterId() + ".jpg")
                        // .likes(artletter.getLikes())
                        // .scraps(artletter.getScraps())
                        .build())
                .toList();

        // PageInfo 생성
        PageInfo pageInfo = new PageInfo(
                artletterPage.getNumber() + 1, // Page number starts from 1
                artletterPage.getSize(),
                artletterPage.hasNext(),
                artletterPage.getTotalElements(),
                artletterPage.getTotalPages()
        );

        // 최종 응답 Map 생성
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("pageInfo", pageInfo); // 최상위 키로 pageInfo 추가
        response.put("pages", result); // 최상위 키로 result 추가

        return response;
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
