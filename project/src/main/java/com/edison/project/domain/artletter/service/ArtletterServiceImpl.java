package com.edison.project.domain.artletter.service;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.response.PageInfo;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.artletter.dto.ArtletterDTO;
import com.edison.project.domain.artletter.dto.CountDto;
import com.edison.project.domain.artletter.entity.Artletter;
import com.edison.project.domain.artletter.entity.ArtletterCategory;
import com.edison.project.domain.artletter.entity.ArtletterLikes;
import com.edison.project.domain.artletter.repository.ArtletterLikesRepository;
import com.edison.project.domain.artletter.repository.ArtletterRepository;
import com.edison.project.domain.member.entity.Member;
import com.edison.project.domain.member.entity.MemberMemory;
import com.edison.project.domain.member.repository.MemberMemoryRepository;
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
    private final MemberMemoryRepository memberMemoryRepository;
    private final ArtletterLikesRepository artletterLikesRepository;
    private final ScrapRepository scrapRepository;
    private final Map<Long, LinkedList<String>> recentSearchKeywords = new HashMap<>();


    // 전체 아트레터 조회 API
    @Override
    public ResponseEntity<ApiResponse> getAllArtlettersResponse(CustomUserPrincipal userPrincipal, int page, int size, String sortType) {
        Page<Artletter> artletters = getPaginatedArtletters(page, size);
        PageInfo pageInfo = buildPageInfo(artletters);

        Member member = memberRepository.findByMemberId(userPrincipal.getMemberId());

        List<ArtletterDTO.SimpleArtletterResponseDto> response = artletters.getContent().stream()
                .map(artletter -> buildSimpleListResponseDto(artletter, member))
                .collect(Collectors.toList());

        response = sortArtletters(response, sortType);
        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, response);
    }

    // 전체 아트레터 조회 api - 페이징된 아트레터 목록 조회
    private Page<Artletter> getPaginatedArtletters(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return artletterRepository.findAll(pageable);
    }

    // 아트레터 등록 api
    @Override
    public ArtletterDTO.CreateResponseDto createArtletter(CustomUserPrincipal userPrincipal, ArtletterDTO.CreateRequestDto request) {

        Artletter artletter = Artletter.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .writer(request.getWriter())
                .readTime(request.getReadTime())
                .tag(request.getTag())
                .category(request.getCategory())
                .thumbnail(request.getThumbnail())
                .build();

        Artletter savedArtletter = artletterRepository.save(artletter);

        return ArtletterDTO.CreateResponseDto.builder()
                .artletterId(savedArtletter.getLetterId())
                .title(savedArtletter.getTitle())
                .thumbnail(savedArtletter.getThumbnail())
                .readTime(savedArtletter.getReadTime())
                .category(savedArtletter.getCategory())
                .tag(savedArtletter.getTag())
                .createdAt(savedArtletter.getCreatedAt())
                .build();
    }


    // 아트레터 좋아요 토글 api
    @Override
    @Transactional
    public ArtletterDTO.LikeResponseDto likeToggleArtletter(CustomUserPrincipal userPrincipal, Long letterId) {

        Member member = memberRepository.findByMemberId(userPrincipal.getMemberId());
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

        Member member = memberRepository.findByMemberId(userPrincipal.getMemberId());
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
    public ResponseEntity<ApiResponse> searchArtletters(CustomUserPrincipal userPrincipal, String keyword, int page, int size, String sortType) {
        Member member = memberRepository.findByMemberId(userPrincipal.getMemberId());

        Pageable pageable = PageRequest.of(page, size);
        Page<Artletter> resultPage = artletterRepository.searchByKeyword(keyword, pageable);

        PageInfo pageInfo = buildPageInfo(resultPage);

        List<Artletter> sortedResults = sortSearchResults(resultPage.getContent(), keyword);
        List<ArtletterDTO.SimpleArtletterResponseDto> response = sortedResults.stream()
                .map(artletter -> buildSimpleListResponseDto(artletter, member))
                .collect(Collectors.toList());

        response = sortArtletters(response, sortType);

        // 🔥 추가된 부분: 최근 검색어 저장
        if (member != null && keyword != null && !keyword.trim().isEmpty()) {
            saveMemoryKeyword(member, keyword);
        }

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

    // 최근 검색어 자동 저장 메서드
    private void saveMemoryKeyword(Member member, String memory) {
        // 올바른 메서드 사용
        List<MemberMemory> memories = memberMemoryRepository.findMemberMemoriesByMemberId(member.getMemberId());

        // 이미 존재하는 메모리 삭제
        memories.stream()
                .filter(existingMemory -> existingMemory.getMemory().equals(memory))
                .findFirst()
                .ifPresent(memberMemoryRepository::delete);

        // 최대 3개 유지, 오래된 것부터 삭제
        if (memories.size() >= 3) {
            memories.sort(Comparator.comparing(MemberMemory::getCreatedAt));
            memberMemoryRepository.delete(memories.get(0));
        }

        // 새로운 메모리 추가
        MemberMemory newMemory = MemberMemory.builder()
                .member(member)
                .memory(memory)
                .build();

        memberMemoryRepository.save(newMemory);
    }


    // 최근 검색어 조회
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMemoryKeyword(CustomUserPrincipal userPrincipal) {
        Long memberId = userPrincipal.getMemberId();

        List<String> memories = memberMemoryRepository.findMemoriesByMemberId(memberId);

        ArtletterDTO.MemoryKeywordResponseDto response = new ArtletterDTO.MemoryKeywordResponseDto(memories);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }


    // 최근 검색어 삭제
// 최근 검색어 삭제
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> deleteMemoryKeyword(CustomUserPrincipal userPrincipal, ArtletterDTO.MemoryKeywordRequestDto request) {
        Long memberId = userPrincipal.getMemberId();
        String keyword = request.getKeyword() != null ? request.getKeyword().trim() : null;

        if (keyword == null || keyword.isEmpty()) {
            throw new GeneralException(ErrorStatus.MEMORY_KEYWORD_NOT_FOUND);
        }

        // 데이터베이스를 통해 검색어 존재 여부 확인
        int deletedCount = memberMemoryRepository.deleteByMemberIdAndMemory(memberId, keyword);

        if (deletedCount == 0) {
            throw new GeneralException(ErrorStatus.MEMORY_KEYWORD_NOT_FOUND);
        }

        return ApiResponse.onSuccess(SuccessStatus._OK);
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


        boolean isLiked = member != null && artletterLikesRepository.existsByMemberAndArtletter(member, artletter);
        int likesCnt = artletterLikesRepository.countByArtletter(artletter);
        boolean isScrapped = member != null && scrapRepository.existsByMemberAndArtletter(member, artletter);
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

        Member member = Optional.ofNullable(userPrincipal)
                .map(up -> memberRepository.findByMemberId(up.getMemberId()))
                .orElse(null);

        List<Long> artletterIds = editorRequestDto.getArtletterIds();
        List<Artletter> artletters = artletterRepository.findByLetterIdIn(artletterIds);

        if (artletters.size() < artletterIds.size()) {
            Set<Long> foundArtletterIds = artletters.stream()
                    .map(Artletter::getLetterId)
                    .collect(Collectors.toSet());

            for (Long artletterId : artletterIds) {
                if (!foundArtletterIds.contains(artletterId)) {
                    throw new GeneralException(ErrorStatus.LETTERS_NOT_FOUND, "요청된 아트레터가 존재하지 않습니다. (ID: " + artletterId + ")");
                }
            }
        }

        Map<Long, Boolean> likedMap = artletterLikesRepository.findByMemberAndArtletterIn(member, artletters)
                .stream().collect(Collectors.toMap(al -> al.getArtletter().getLetterId(), al -> true));

        Map<Long, Boolean> scrappedMap = scrapRepository.findByMemberAndArtletterIn(member, artletters)
                .stream().collect(Collectors.toMap(sc -> sc.getArtletter().getLetterId(), sc -> true));

        Map<Long, Integer> likesCountMap = artletterLikesRepository.countByArtletterIn(artletters)
                .stream()
                .collect(Collectors.toMap(CountDto::getArtletterId, countDto -> countDto.getCount().intValue()));

        Map<Long, Integer> scrapsCountMap = scrapRepository.countByArtletterIn(artletters)
                .stream()
                .collect(Collectors.toMap(CountDto::getArtletterId, countDto -> countDto.getCount().intValue()));

        List<ArtletterDTO.ListResponseDto> artletterList = artletters.stream()
                .map(artletter -> ArtletterDTO.ListResponseDto.builder()
                        .artletterId(artletter.getLetterId())
                        .title(artletter.getTitle())
                        .content(artletter.getContent())
                        .tags(artletter.getTag())
                        .writer(artletter.getWriter())
                        .category(artletter.getCategory())
                        .readTime(artletter.getReadTime())
                        .thumbnail(artletter.getThumbnail())
                        .likesCnt(likesCountMap.getOrDefault(artletter.getLetterId(), 0))
                        .scrapsCnt(scrapsCountMap.getOrDefault(artletter.getLetterId(), 0))
                        .isLiked(likedMap.getOrDefault(artletter.getLetterId(), false))
                        .isScraped(scrappedMap.getOrDefault(artletter.getLetterId(), false))
                        .createdAt(artletter.getCreatedAt())
                        .updatedAt(artletter.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());


        return ApiResponse.onSuccess(SuccessStatus._OK, artletterList);
    }


    // 추천바 - 카테고리 조회 api
    @Override
    @Transactional
    public List<String> getRecommendCategory() {
        return Arrays.asList("기술과학", "자연과학", "교육");
    }


    // 추천바 - 키워드 조회 api
    @Override
    @Transactional
    public List<ArtletterDTO.recommendKeywordDto> getRecommendKeyword(List<Long> artletterIds) {
        List<Artletter> artletters = validateArtletterIds(artletterIds);

        return artletters.stream()
                .map(artletter -> {
                    if (artletter.getKeyword() == null) {
                        throw new GeneralException(ErrorStatus.KEYWORD_IS_EMPTY);
                    }
                    return new ArtletterDTO.recommendKeywordDto(artletter.getLetterId(), artletter.getKeyword());
                })
                .collect(Collectors.toList());
    }


    @Override
    public ResponseEntity<ApiResponse> getScrapArtlettersByCategory(CustomUserPrincipal userPrincipal, Pageable pageable) {

        Member member = memberRepository.findByMemberId(userPrincipal.getMemberId());

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
                                    .scrappedAt(scrap.getCreatedAt())
                                    .build();
                        }).toList()
                )).toList();

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, groupedArtletters);
    }

    @Override
    public ResponseEntity<ApiResponse> getScrapCategoryArtletters(CustomUserPrincipal userPrincipal, ArtletterCategory category, Pageable pageable) {

        Member member = memberRepository.findByMemberId(userPrincipal.getMemberId());

        try {
            ArtletterCategory artletterCategory = ArtletterCategory.valueOf(String.valueOf(category));
        } catch (IllegalArgumentException e) {
            throw new GeneralException(ErrorStatus.NOT_EXISTS_CATEGORY);
        }

        Page<Scrap> scraps = scrapRepository.findByMemberAndArtletter_Category(member, category, pageable);

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
                            .scrappedAt(scrap.getCreatedAt())
                            .build();
                }).toList();

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, artletters);
    }




    /*
    공통 메서드 모음
    */

    // Artletter 존재 여부 조회
    private Artletter findArtletterById(Long letterId) {
        return artletterRepository.findById(letterId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LETTERS_NOT_FOUND));
    }

    // 페이지 정보 생성
    private PageInfo buildPageInfo(Page<Artletter> artletters) {
        return new PageInfo(
                artletters.getNumber(),
                artletters.getSize(),
                artletters.hasNext(),
                artletters.getTotalElements(),
                artletters.getTotalPages()
        );
    }

    // 아트레터 DTO 생성
    private ArtletterDTO.SimpleArtletterResponseDto buildSimpleListResponseDto(Artletter artletter, Member member) {
        boolean isLiked = member != null && artletterLikesRepository.existsByMemberAndArtletter(member, artletter);
        int likesCnt = artletterLikesRepository.countByArtletter(artletter);
        boolean isScrapped = member != null && scrapRepository.existsByMemberAndArtletter(member, artletter);
        int scrapCnt = scrapRepository.countByArtletter(artletter);

        return ArtletterDTO.SimpleArtletterResponseDto.builder()
                .artletterId(artletter.getLetterId())
                .title(artletter.getTitle())
                .thumbnail(artletter.getThumbnail())
                .likesCnt(likesCnt)
                .scrapsCnt(scrapCnt)
                .isLiked(isLiked)
                .isScraped(isScrapped)
                .updatedAt(artletter.getUpdatedAt())
                .build();
    }

    // 아트레터 스크랩/좋아요/최신순 정렬
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

    // 추천바 - 아트레터 요청 검증
    private List<Artletter> validateArtletterIds(List<Long> artletterIds) {
        if (artletterIds == null || artletterIds.isEmpty() || artletterIds.size() > 3) {
            throw new GeneralException(ErrorStatus.INVALID_ARTLETTER_REQUEST);
        }

        Set<Long> uniqueIds = new HashSet<>(artletterIds);
        if (uniqueIds.size() != artletterIds.size()) {
            throw new GeneralException(ErrorStatus.DUPLICATE_ARTLETTER_IDS);
        }

        List<Artletter> artletters = artletterRepository.findByLetterIdIn(artletterIds);
        if (artletters.size() != artletterIds.size()) {
            throw new GeneralException(ErrorStatus.LETTERS_NOT_FOUND);
        }

        return artletters;
    }

    @Override
    public ResponseEntity<ApiResponse> getArtlettersByCategory(CustomUserPrincipal userPrincipal, ArtletterCategory category, Pageable pageable) {
        Member member = (userPrincipal != null) ? memberRepository.findByMemberId(userPrincipal.getMemberId()) : null;

        // 카테고리 유효성 확인
        if (category == null) {
            throw new GeneralException(ErrorStatus.NOT_EXISTS_CATEGORY);
        }

        // 카테고리별 아트레터 조회
        Page<Artletter> artletters = artletterRepository.findByCategory(category, pageable);

        PageInfo pageInfo = new PageInfo(
                artletters.getNumber(),
                artletters.getSize(),
                artletters.hasNext(),
                artletters.getTotalElements(),
                artletters.getTotalPages()
        );

        List<ArtletterDTO.ListResponseDto> response = artletters.getContent().stream()
                .map(artletter -> {
                    boolean isScrapped = (member != null)git && scrapRepository.existsByMemberAndArtletter(member, artletter);

                    return ArtletterDTO.ListResponseDto.builder()
                            .artletterId(artletter.getLetterId())
                            .title(artletter.getTitle())
                            .thumbnail(artletter.getThumbnail())
                            .tags(artletter.getTag())
                            .isScraped(isScrapped)
                            .build();
                }).toList();

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, response);
    }
}