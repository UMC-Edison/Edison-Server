package com.edison.project.domain.artletter.service;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.response.PageInfo;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.artletter.dto.ArtletterDTO;
import com.edison.project.domain.artletter.entity.Artletter;
import com.edison.project.domain.artletter.entity.ArtletterLikes;
import com.edison.project.domain.artletter.repository.ArtletterLikesRepository;
import com.edison.project.domain.artletter.repository.ArtletterRepository;
import com.edison.project.domain.member.entity.Member;
import com.edison.project.domain.member.repository.MemberRepository;
import com.edison.project.domain.scrap.entity.Scrap;
import com.edison.project.domain.scrap.repository.ScrapRepository;
import com.edison.project.global.security.CustomUserPrincipal;
import jakarta.transaction.Transactional;
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
    private final MemberRepository memberRepository;
    private final ArtletterLikesRepository artletterLikesRepository;
    private final ScrapRepository scrapRepository;

    public Page<Artletter> getAllArtletters(int page, int size) {

        // 페이지 요청 생성
        Pageable pageable = PageRequest.of(page, size);

        return artletterRepository.findAll(pageable);
    }


    @Override
    public ArtletterDTO.CreateResponseDto createArtletter(CustomUserPrincipal userPrincipal, ArtletterDTO.CreateRequestDto request) {

        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

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
                .likes(artletterLikesRepository.countByArtletter(artletter))
                .scraps(scrapRepository.countByArtletter(artletter))
                .isScrap(scrapRepository.existsByMemberAndArtletter(member, artletter))
                .build();
    }

    @Override
    @Transactional
    public ArtletterDTO.LikeResponseDto likeToggleArtletter(CustomUserPrincipal userPrincipal, Long letterId) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Artletter artletter = artletterRepository.findById(letterId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LETTERS_NOT_FOUND));

        boolean alreadyLiked = artletterLikesRepository.existsByMemberAndArtletter(member, artletter);

        if (alreadyLiked) {
            // 좋아요 취소
            artletterLikesRepository.deleteByMemberAndArtletter(member, artletter);
        } else {
            // 좋아요
            ArtletterLikes like = ArtletterLikes.builder()
                    .member(member)
                    .artletter(artletter)
                    .build();

            artletterLikesRepository.save(like);
        }

        int likeCnt = artletterLikesRepository.countByArtletter(artletter);

        return ArtletterDTO.LikeResponseDto.builder()
                .artletterId(letterId)
                .likesCnt(likeCnt)
                .isLiked(!alreadyLiked)
                .build();
    }

    @Override
    public ArtletterDTO.ScrapResponseDto scrapToggleArtletter(CustomUserPrincipal userPrincipal, Long letterId) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Artletter artletter = artletterRepository.findById(letterId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LETTERS_NOT_FOUND));

        boolean alreadyScrapped = scrapRepository.existsByMemberAndArtletter(member, artletter);

        if (alreadyScrapped) {
            scrapRepository.deleteByMemberAndArtletter(member, artletter);
        } else {
            Scrap scrap = Scrap.builder()
                    .member(member)
                    .artletter(artletter)
                    .build();

            scrapRepository.save(scrap);
        }

        int scrapCnt = scrapRepository.countByArtletter(artletter);

        return ArtletterDTO.ScrapResponseDto.builder()
                .artletterId(letterId)
                .scrapsCnt(scrapCnt)
                .isScrapped(!alreadyScrapped)
                .build();
    }

    @Override
    public Page<Artletter> searchArtletters(String keyword, Pageable pageable) {
        return artletterRepository.searchByKeyword(keyword, pageable);
    }


    @Override
    public ArtletterDTO.ListResponseDto getArtletter(CustomUserPrincipal userPrincipal, long letterId) {

        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }


        Artletter artletter = artletterRepository.findById(letterId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LETTERS_NOT_FOUND));

        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        boolean isLiked = artletterLikesRepository.existsByMemberAndArtletter(member, artletter);
        int likesCnt = artletterLikesRepository.countByArtletter(artletter);
        boolean isScrapped = scrapRepository.existsByMemberAndArtletter(member, artletter);
        int scrapCnt = scrapRepository.countByArtletter(artletter);

        return ArtletterDTO.ListResponseDto.builder()
                .artletterId(artletter.getLetterId())
                .title(artletter.getTitle())
                .content(artletter.getContent())
                .tags(artletter.getTag())
                .writer(artletter.getWriter())
                .category(String.valueOf(artletter.getCategory()))
                .readTime(artletter.getReadTime())
                .thumbnail(artletter.getThumbnail())
                .likesCnt(likesCnt)
                .scrapsCnt(scrapCnt)
                .isLiked(isLiked)
                .isScraped(isScrapped)
                .createdAt(artletter.getCreatedAt())
                .updatedAt(artletter.getUpdatedAt())
                .build();
    }

    @Override
    public ResponseEntity<ApiResponse> getEditorArtletters(CustomUserPrincipal userPrincipal, ArtletterDTO.EditorRequestDto editorRequestDto) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }
        if (editorRequestDto == null || editorRequestDto.getArtletterIds() == null || editorRequestDto.getArtletterIds().isEmpty()) {
            throw new GeneralException(ErrorStatus.ARTLETTER_ID_REQUIRED);
        }
        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        List<Long> artletterIds = editorRequestDto.getArtletterIds();
        List<Artletter> artletters = artletterRepository.findByLetterIdIn(artletterIds);

        Set<Long> foundArtletterIds = artletters.stream()
                .map(Artletter::getLetterId)  // Artletter에서 letterId 추출
                .collect(Collectors.toSet());

        for (Long artletterId : artletterIds) {
            if (!foundArtletterIds.contains(artletterId)) {
                throw new GeneralException(ErrorStatus.LETTERS_NOT_FOUND, "요청된 아트레터가 존재하지 않습니다. (ID: " + artletterId + ")");
            }
        }

        List<ArtletterDTO.ListResponseDto> artletterList = artletterRepository.findByLetterIdIn(editorRequestDto.getArtletterIds()).stream()
                .map(artletter -> {
                    boolean isLiked = artletterLikesRepository.existsByMemberAndArtletter(member, artletter);
                    boolean isScrapped = scrapRepository.existsByMemberAndArtletter(member, artletter);
                    int likesCnt = artletterLikesRepository.countByArtletter(artletter);
                    int scrapsCnt = scrapRepository.countByArtletter(artletter);

                    return ArtletterDTO.ListResponseDto.builder()
                            .artletterId(artletter.getLetterId())
                            .title(artletter.getTitle())
                            .content(artletter.getContent())
                            .tags(artletter.getTag())
                            .writer(artletter.getWriter())
                            .category(String.valueOf(artletter.getCategory()))
                            .readTime(artletter.getReadTime())
                            .thumbnail(artletter.getThumbnail())
                            .likesCnt(likesCnt)
                            .scrapsCnt(scrapsCnt)
                            .isLiked(isLiked)
                            .isScraped(isScrapped)
                            .createdAt(artletter.getCreatedAt())
                            .updatedAt(artletter.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return ApiResponse.onSuccess(SuccessStatus._OK, artletterList);
    }

    @Override
    public ResponseEntity<ApiResponse> getScrapArtletter(CustomUserPrincipal userPrincipal, Pageable pageable) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Page<Scrap> scraps = scrapRepository.findByMember(member, pageable);

        PageInfo pageInfo = new PageInfo(
                scraps.getNumber(),
                scraps.getSize(),
                scraps.hasNext(),
                scraps.getTotalElements(),
                scraps.getTotalPages()
        );

        List<ArtletterDTO.MyScrapResponseDto> artletters = scraps.getContent().stream()
                .map(scrap -> {
                    Artletter artletter = scrap.getArtletter();
                    int likesCnt = artletterLikesRepository.countByArtletter(artletter);
                    int scrapsCnt = scrapRepository.countByArtletter(artletter);
                    return ArtletterDTO.MyScrapResponseDto.builder()
                            .artletterId(artletter.getLetterId())
                            .title(artletter.getTitle())
                            .thumbnail(artletter.getThumbnail())
                            .likesCnt(likesCnt)
                            .scrapsCnt(scrapsCnt)
                            .scrappedAt(artletter.getCreatedAt())
                            .build();
                }).toList();

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, artletters);
    }

}
