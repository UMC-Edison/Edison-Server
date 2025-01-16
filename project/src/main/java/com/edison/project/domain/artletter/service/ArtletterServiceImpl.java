package com.edison.project.domain.artletter.service;

import com.edison.project.common.response.PageInfo;
import com.edison.project.domain.artletter.dto.ArtletterDTO;
import com.edison.project.domain.artletter.dto.TestDTO;
import com.edison.project.domain.artletter.entity.Artletter;
import com.edison.project.domain.artletter.repository.ArtletterRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArtletterServiceImpl implements ArtletterService {

    private final ArtletterRepository artletterRepository;

    public ArtletterServiceImpl(ArtletterRepository artletterRepository) {
        this.artletterRepository = artletterRepository;
    }

    @Override
    public TestDTO getAllArtletters(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Artletter> artletterPage = artletterRepository.findAll(pageable);

        // Artletter -> ArtletterDTO.ListResponseDto 변환
        List<ArtletterDTO.ListResponseDto> result = artletterPage.getContent().stream()
                .map(artletter -> ArtletterDTO.ListResponseDto.builder()
                        .artletterId(artletter.getLetterId())
                        .title(artletter.getTitle())
                        .thumbnail("thumbnail_url_placeholder") // 썸네일 로직 추가 예정
                        // .likes(artletter.getLikes())
                        // .scraps(artletter.getScraps())
                        .build())
                .toList();

        // 페이지 정보 생성
        PageInfo pageInfo = new PageInfo(
                artletterPage.getNumber(),
                artletterPage.getSize(),
                artletterPage.hasNext(),
                artletterPage.getTotalElements(),
                artletterPage.getTotalPages()
        );

        // TestDTO 생성
        return new TestDTO(true, 200, "요청이 성공적으로 처리되었습니다.", pageInfo, result);
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
