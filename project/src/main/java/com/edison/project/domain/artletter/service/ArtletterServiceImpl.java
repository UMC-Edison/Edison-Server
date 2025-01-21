package com.edison.project.domain.artletter.service;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.artletter.dto.ArtletterDTO;
import com.edison.project.domain.artletter.entity.Artletter;
import com.edison.project.domain.artletter.repository.ArtletterLikesRepository;
import com.edison.project.domain.artletter.repository.ArtletterRepository;
import com.edison.project.domain.scrap.repository.ScrapRepository;
import com.edison.project.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtletterServiceImpl implements ArtletterService {

    private final ArtletterRepository artletterRepository;
    private final ArtletterLikesRepository artletterLikesRepository;
    private final ScrapRepository scrapRepository;

    public Page<Artletter> getAllArtletters(CustomUserPrincipal userPrincipal, int page, int size) {
        // 페이지 요청 생성
        Pageable pageable = PageRequest.of(page, size);

        return artletterRepository.findAll(pageable);
    }


    @Override
    public ArtletterDTO.CreateResponseDto createArtletter(CustomUserPrincipal userPrincipal, ArtletterDTO.CreateRequestDto request) {
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
                .likes(artletterLikesRepository.countByArtletter_LetterId(artletter.getLetterId()))
                .scraps(scrapRepository.countByArtletter_LetterId(artletter.getLetterId()))
                .isScrap(scrapRepository.existsByArtletter_LetterIdAndMember_MemberId(artletter.getLetterId(), userPrincipal.getMemberId()))
                .build();
    }

    @Override
    public Page<Artletter> searchArtletters(CustomUserPrincipal userPrincipal, String keyword, Pageable pageable) {
        return artletterRepository.searchByKeyword(keyword, pageable);
    }


    public ResponseEntity<ApiResponse> getEditorArtletters(CustomUserPrincipal userPrincipal,  ArtletterDTO.EditorRequestDto editorRequestDto) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        List<Long> artletterIds = editorRequestDto.getArtletterIds();

        if (artletterIds == null || artletterIds.isEmpty()) {
            throw new GeneralException(ErrorStatus.ARTLETTER_ID_REQUIRED);
        }

        List<ArtletterDTO.ListResponseDto> artletters = artletterRepository.findByLetterIdIn(artletterIds).stream()
                .map(artletter -> ArtletterDTO.ListResponseDto.builder()
                        .artletterId(artletter.getLetterId())
                        .title(artletter.getTitle())
                        .thumbnail(artletter.getThumbnail())
                        .likes(artletterLikesRepository.countByArtletter_LetterId(artletter.getLetterId()))
                        .scraps(scrapRepository.countByArtletter_LetterId(artletter.getLetterId()))
                        .isScrap(scrapRepository.existsByArtletter_LetterIdAndMember_MemberId(artletter.getLetterId(), userPrincipal.getMemberId()))
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.onSuccess(SuccessStatus._OK, artletters);
    }

}
