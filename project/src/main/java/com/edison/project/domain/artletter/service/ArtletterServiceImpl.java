package com.edison.project.domain.artletter.service;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.response.PageInfo;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.artletter.dto.ArtletterDTO;
import com.edison.project.domain.artletter.entity.Artletter;
import com.edison.project.domain.artletter.entity.ArtletterCategory;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArtletterServiceImpl implements ArtletterService {

    private final ArtletterRepository artletterRepository;
    private final MemberRepository memberRepository;
    private final ArtletterLikesRepository artletterLikesRepository;
    private final ScrapRepository scrapRepository;





    // 전체 아트레터 조회 API
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getAllArtlettersResponse(
            CustomUserPrincipal userPrincipal, int page, int size, String sortType) {

        Member member = null; // 로그인하지 않은 경우 null 유지
        if (userPrincipal != null) {
            member = memberRepository.findById(userPrincipal.getMemberId()).orElse(null);
        }

        Page<Artletter> artletters = getPaginatedArtletters(page, size);
        PageInfo pageInfo = buildPageInfo(artletters);

        List<ArtletterDTO.SimpleArtletterResponseDto> response =
                extractSimplifiedArtletters(artletters.getContent(), member);

        response = sortArtletters(response, sortType);

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, response);
    }

    // 전체 아트레터 조회 api - 페이징된 아트레터 목록 조회
    private Page<Artletter> getPaginatedArtletters(int page, int size) {
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



    // 아트레터 좋아요 토글 api
    @Override
    @Transactional
    public ArtletterDTO.LikeResponseDto likeToggleArtletter(CustomUserPrincipal userPrincipal, Long letterId) {

        Member member = null;
        if (userPrincipal != null) {
            member = memberRepository.findById(userPrincipal.getMemberId()).orElse(null);
        }

        Artletter artletter = findArtletterById(letterId);
        boolean alreadyLiked = artletterLikesRepository.existsByMemberAndArtletter(member, artletter);

        toggleLikeStatus(member, artletter, alreadyLiked);
        int likeCnt = artletterLikesRepository.countByArtletter(artletter);

        return buildLikeResponseDto(letterId, likeCnt, !alreadyLiked);
    }

    // 아트레터 좋아요 토글 api - 좋아요 토글 메서드 분리
    private void toggleLikeStatus(Member member, Artletter artletter, boolean alreadyLiked) {
        if (alreadyLiked) {
            artletterLikesRepository.deleteByMemberAndArtletter(member, artletter);
        } else {
            artletterLikesRepository.save(ArtletterLikes.builder()
                    .member(member)
                    .artletter(artletter)
                    .build());
        }
    }

    // 아트레터 좋아요 토글 api - 결과 생성 메서드 분리
    private ArtletterDTO.LikeResponseDto buildLikeResponseDto(Long letterId, int likeCnt, boolean isLiked) {
        return ArtletterDTO.LikeResponseDto.builder()
                .artletterId(letterId)
                .likesCnt(likeCnt)
                .isLiked(isLiked)
                .build();
    }



    // 아트레터 스크랩 토글 api
    @Override
    @Transactional
    public ArtletterDTO.ScrapResponseDto scrapToggleArtletter(CustomUserPrincipal userPrincipal, Long letterId) {

        Member member = findMemberById(userPrincipal.getMemberId());
        Artletter artletter = findArtletterById(letterId);
        boolean alreadyScrapped = scrapRepository.existsByMemberAndArtletter(member, artletter);

        toggleScrap(member, artletter, alreadyScrapped);
        int scrapCnt = scrapRepository.countByArtletter(artletter);

        return buildScrapResponseDto(letterId, scrapCnt, !alreadyScrapped);
    }

    // 아트레터 스크랩 토글 api - 스크랩 토글 메서드 분리
    private void toggleScrap(Member member, Artletter artletter, boolean alreadyScrapped) {
        if (alreadyScrapped) {
            scrapRepository.deleteByMemberAndArtletter(member, artletter);
        } else {
            scrapRepository.save(Scrap.builder().member(member).artletter(artletter).build());
        }
    }

    // 아트레터 스크랩 토글 api - 결과 생성 메서드 분리
    private ArtletterDTO.ScrapResponseDto buildScrapResponseDto(Long letterId, int scrapCnt, boolean isScrapped) {
        return ArtletterDTO.ScrapResponseDto.builder()
                .artletterId(letterId)
                .scrapsCnt(scrapCnt)
                .isScrapped(isScrapped)
                .build();
    }



    // 아트레터 검색 api
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> searchArtletters(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal, String keyword, int page, int size, String sortType) {

        Member member = null;
        if (userPrincipal != null) {
            member = memberRepository.findById(userPrincipal.getMemberId()).orElse(null);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Artletter> resultPage = artletterRepository.searchByKeyword(keyword, pageable);
        PageInfo pageInfo = buildPageInfo(resultPage);

        List<Artletter> sortedResults = sortSearchResults(resultPage.getContent(), keyword);
        List<ArtletterDTO.SimpleArtletterResponseDto> response = extractSimplifiedArtletters(sortedResults, member);

        response = sortArtletters(response, sortType);

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, response);
    }

    // 아트레터 검색 api - 검색 결과 기본 정렬
    private List<Artletter> sortSearchResults(List<Artletter> artletters, String keyword) {
        return artletters.stream()
                .sorted(Comparator
                        .comparing((Artletter a) -> a.getTag() != null && a.getTag().contains(keyword) ? 0 : 1)
                        .thenComparing(a -> a.getTitle() != null && a.getTitle().contains(keyword) ? 0 : 1)
                        .thenComparing(a -> a.getContent() != null && a.getContent().contains(keyword) ? 0 : 1)
                )
                .collect(Collectors.toList());
    }



    // 아트레터 상세조회 api
    @Override
    public ArtletterDTO.ListResponseDto getArtletter(CustomUserPrincipal userPrincipal, long letterId) {

        Artletter artletter = artletterRepository.findById(letterId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LETTERS_NOT_FOUND));

        Member member;
        if (userPrincipal != null) { //로그인한 경우에만 member 조회
            member = memberRepository.findById(userPrincipal.getMemberId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        } else {
            member = null;
        }


        boolean isLiked = artletterLikesRepository.existsByMemberAndArtletter(member, artletter);
        int likesCnt = artletterLikesRepository.countByArtletter(artletter);
        boolean isScrapped = scrapRepository.existsByMemberAndArtletter(member, artletter);
        int scrapCnt = scrapRepository.countByArtletter(artletter);

        return buildListResponseDto(artletter, likesCnt, scrapCnt, isLiked, isScrapped);
    }

    // 아트레터 상세조회 api - 결과 조회 메서드 분리
    private ArtletterDTO.ListResponseDto buildListResponseDto(Artletter artletter, int likesCnt, int scrapCnt, boolean isLiked, boolean isScrapped) {
        return ArtletterDTO.ListResponseDto.builder()
                .artletterId(artletter.getLetterId())
                .title(artletter.getTitle())
                .content(artletter.getContent())
                .tags(artletter.getTag())
                .writer(artletter.getWriter())
                .category(artletter.getCategory())
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

        if (editorRequestDto == null || editorRequestDto.getArtletterIds() == null || editorRequestDto.getArtletterIds().isEmpty()) {
            throw new GeneralException(ErrorStatus.ARTLETTER_ID_REQUIRED);
        }

        Member member;
        if (userPrincipal != null) { //로그인한 경우에만 member 조회
            member = memberRepository.findById(userPrincipal.getMemberId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        } else {
            member = null;
        }

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
                            .category(artletter.getCategory())
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

    @Override
    public List<ArtletterDTO.recommendCategoryDto> getRecommendCategory(List<Long> artletterIds) {
        List<Artletter> artletters = artletterRepository.findByLetterIdIn(artletterIds);

        if (artletters.size() != artletterIds.size()) {
            throw new GeneralException(ErrorStatus.LETTERS_NOT_FOUND);
        }
        return artletters.stream()
                .map(artletter -> new ArtletterDTO.recommendCategoryDto(artletter.getLetterId(), artletter.getCategory()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ArtletterDTO.recommendKeywordDto> getRecommendKeyword(List<Long> artletterIds) {
        List<Artletter> artletters = artletterRepository.findByLetterIdIn(artletterIds);

        if (artletters.size() != artletterIds.size()) {
            throw new GeneralException(ErrorStatus.LETTERS_NOT_FOUND);
        }
        return artletters.stream()
                .map(artletter -> new ArtletterDTO.recommendKeywordDto(artletter.getLetterId(),artletter.getKeyword()))
                .collect(Collectors.toList());
    }

    @Override
    public ResponseEntity<ApiResponse> getScrapArtlettersByCategory(CustomUserPrincipal userPrincipal, Pageable pageable) {

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

        // 카테고리별 그룹화
        Map<String, List<Scrap>> groupedByCategory = scraps.getContent().stream()
                .collect(Collectors.groupingBy(scrap -> String.valueOf(scrap.getArtletter().getCategory())));

        // 그룹화된 데이터를 DTO 리스트로 변환
        List<ArtletterDTO.GroupedScrapResponseDto> groupedArtletters = groupedByCategory.entrySet().stream()
                .map(entry -> new ArtletterDTO.GroupedScrapResponseDto(
                        entry.getKey(),
                        entry.getValue().stream().map(scrap -> {
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
                        }).toList()
                )).toList();

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, groupedArtletters);
    }

    @Override
    public ResponseEntity<ApiResponse> getScrapCategoryArtletters(CustomUserPrincipal userPrincipal, ArtletterCategory category, Pageable pageable) {

        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        try {
            ArtletterCategory artletterCategory = ArtletterCategory.valueOf(String.valueOf(category));
        } catch (IllegalArgumentException e) {
            throw new GeneralException(ErrorStatus.NOT_EXISTS_CATEGORY);
        }

        Page<Scrap> scraps = scrapRepository.findByMemberAndArtletter_Category(member, category, pageable);

//        // 스크랩한 아트레터가 없는 경우 예외 발생
//        if (scraps.isEmpty() || scraps==null) {
//            throw new GeneralException(ErrorStatus.ARTLETTER_NOT_FOUND);
//        }

        PageInfo pageInfo = new PageInfo(
                scraps.getNumber(),
                scraps.getSize(),
                scraps.hasNext(),
                scraps.getTotalElements(),
                scraps.getTotalPages()
        );

        // DTO 변환 전에 엔티티 기준으로 카테고리별 그룹화
        Map<String, List<Scrap>> groupedByCategory = scraps.getContent().stream()
                .collect(Collectors.groupingBy(scrap -> String.valueOf(scrap.getArtletter().getCategory())));

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

    // Member 조회 메서드 분리
    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    // Artletter 조회 메서드 분리
    private Artletter findArtletterById(Long letterId) {
        return artletterRepository.findById(letterId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LETTERS_NOT_FOUND));
    }

    // [공통 메소드] 페이지 정보 생성
    private PageInfo buildPageInfo(Page<Artletter> artletters) {
        return new PageInfo(
                artletters.getNumber(),
                artletters.getSize(),
                artletters.hasNext(),
                artletters.getTotalElements(),
                artletters.getTotalPages()
        );
    }


    // [공통 메소드] 아트레터 필드 추출
    private List<ArtletterDTO.SimpleArtletterResponseDto> extractSimplifiedArtletters(List<Artletter> artletters, Member member) {
        return artletters.stream()
                .map(artletter -> {
                    boolean isScrapped = (member != null && scrapRepository.existsByMemberAndArtletter(member, artletter));

                    log.info("📌 artletterId: {}, memberId: {}, isScrapped: {}",
                            artletter.getLetterId(),
                            (member != null) ? member.getMemberId() : "비로그인",
                            isScrapped);

                    return ArtletterDTO.SimpleArtletterResponseDto.builder()
                            .artletterId(artletter.getLetterId())
                            .title(artletter.getTitle())
                            .thumbnail(artletter.getThumbnail())
                            .likesCnt(artletterLikesRepository.countByArtletter(artletter))
                            .scrapsCnt(scrapRepository.countByArtletter(artletter))
                            .isScrapped(isScrapped)
                            .updatedAt(artletter.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // [공통 메소드] Artletter 정렬 메서드
    private List<ArtletterDTO.SimpleArtletterResponseDto> sortArtletters(List<ArtletterDTO.SimpleArtletterResponseDto> artletters, String sortType) {
        return switch (sortType) {
            case "likes" -> artletters.stream()
                    .sorted(Comparator.comparing(ArtletterDTO.SimpleArtletterResponseDto::getLikesCnt)
                            .reversed())
                    .toList();

            case "scraps" -> artletters.stream()
                    .sorted(Comparator
                            .comparing(ArtletterDTO.SimpleArtletterResponseDto::getScrapsCnt).reversed()
                            .thenComparing(ArtletterDTO.SimpleArtletterResponseDto::getUpdatedAt).reversed()
                            .thenComparing(ArtletterDTO.SimpleArtletterResponseDto::getLikesCnt).reversed())
                    .toList();

            case "latest" -> artletters.stream()
                    .sorted(Comparator.comparing(ArtletterDTO.SimpleArtletterResponseDto::getUpdatedAt).reversed())
                    .toList();

            default -> artletters;
        };
    }


}