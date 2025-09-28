package com.edison.project.domain.artletter.service;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.response.PageInfo;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.artletter.dto.ArtletterDTO;
import com.edison.project.domain.artletter.dto.CountDto;
import com.edison.project.domain.artletter.entity.*;
import com.edison.project.domain.artletter.repository.ArtletterLikesRepository;
import com.edison.project.domain.artletter.repository.ArtletterRepository;
import com.edison.project.domain.artletter.repository.EditorPickRepository;
import com.edison.project.domain.artletter.repository.WriterRepository;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
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
    private final EditorPickRepository editorPickRepository;
    private final WriterRepository writerRepository;

    // 전체 아트레터 조회 API
    @Override
    public ResponseEntity<ApiResponse> getAllArtlettersResponse(CustomUserPrincipal userPrincipal, int page, int size, String sortType) {
        Pageable pageable = switch (sortType) {
            case "likes" -> PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "likesCount"));
            case "scraps" -> PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scrapsCount"));
            case "latest" -> PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            default -> PageRequest.of(page, size); // 정렬 없음
        };

        Page<Artletter> artletters = artletterRepository.findAll(pageable);
        PageInfo pageInfo = buildPageInfo(artletters);

        Member member = getMemberIfAuthenticated(userPrincipal);

        List<ArtletterDTO.SimpleArtletterResponseDto> response = artletters.getContent().stream()
                .map(artletter -> buildSimpleListResponseDto(artletter, member))
                .collect(Collectors.toList());

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, response);
    }

    // 아트레터 등록 api
    @Override
    public ArtletterDTO.CreateResponseDto createArtletter(CustomUserPrincipal userPrincipal, ArtletterDTO.CreateRequestDto request) {

        Writer writer = writerRepository.getReferenceById(request.getWriterId());
        // 존재 여부 보장 안해도 되니까 .. 레퍼런스 썻어요

        Artletter artletter = Artletter.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .writer(writer)
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
        boolean alreadyScraped = scrapRepository.existsByMemberAndArtletter(member, artletter);

        toggleScrap(member, artletter, alreadyScraped);
        int scrapCnt = scrapRepository.countByArtletter(artletter);

        return buildScrapResponseDto(letterId, scrapCnt, !alreadyScraped);
    }

    // 아트레터 스크랩 토글 api - 스크랩 토글 메서드 분리
    private void toggleScrap(Member member, Artletter artletter, boolean alreadyScraped) {
        if (alreadyScraped) {
            scrapRepository.deleteByMemberAndArtletter(member, artletter);
        } else {
            scrapRepository.save(Scrap.builder().member(member).artletter(artletter).build());
        }
    }

    // 아트레터 스크랩 토글 api - 결과 생성 메서드 분리
    private ArtletterDTO.ScrapResponseDto buildScrapResponseDto(Long letterId, int scrapCnt, boolean isScraped) {
        return ArtletterDTO.ScrapResponseDto.builder()
                .artletterId(letterId)
                .scrapsCnt(scrapCnt)
                .isScraped(isScraped)
                .build();
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> searchArtletters(CustomUserPrincipal userPrincipal, String keyword, int page, int size, String sortType) {
        Member member = getMemberIfAuthenticated(userPrincipal);

        Pageable pageable = switch (sortType) {
            case "likes" -> PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "likesCount"));
            case "scraps" -> PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scrapsCount"));
            case "latest" -> PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            default -> // relevance: native query에서 ORDER BY로 처리
                    PageRequest.of(page, size);
        };

        Page<Artletter> resultPage = artletterRepository.searchByKeyword(keyword, pageable);
        PageInfo pageInfo = buildPageInfo(resultPage);

        List<ArtletterDTO.SimpleArtletterResponseDto> response = resultPage.getContent().stream()
                .map(artletter -> buildSimpleListResponseDto(artletter, member))
                .collect(Collectors.toList());

        // 최근 검색어 저장
        if (member != null && keyword != null && !keyword.trim().isEmpty()) {
            saveMemoryKeyword(member, keyword);
        }

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, response);
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
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> deleteMemoryKeyword(CustomUserPrincipal userPrincipal, String keyword) {
        Long memberId = userPrincipal.getMemberId();

        keyword = keyword != null ? keyword.trim() : null;

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
        boolean isScraped = member != null && scrapRepository.existsByMemberAndArtletter(member, artletter);
        int scrapCnt = scrapRepository.countByArtletter(artletter);

        return buildListResponseDto(artletter, likesCnt, scrapCnt, isLiked, isScraped);
    }

    // 아트레터 상세조회 api - 결과 조회 메서드 분리
    private ArtletterDTO.ListResponseDto buildListResponseDto(Artletter artletter, int likesCnt, int scrapCnt, boolean isLiked, boolean isScraped) {
        return ArtletterDTO.ListResponseDto.builder()
                .artletterId(artletter.getLetterId())
                .title(artletter.getTitle())
                .content(artletter.getContent())
                .tags(artletter.getTag())
                .writerSummary( toWriterSummaryDto(artletter.getWriter()) )
                .category(artletter.getCategory())
                .readTime(artletter.getReadTime())
                .thumbnail(artletter.getThumbnail())
                .likesCnt(likesCnt)
                .scrapsCnt(scrapCnt)
                .isLiked(isLiked)
                .isScraped(isScraped)
                .createdAt(artletter.getCreatedAt())
                .updatedAt(artletter.getUpdatedAt())
                .build();
    }

    @Override
    public List<ArtletterDTO.ListResponseDto> getEditorArtletters(CustomUserPrincipal userPrincipal) {

        Member member = Optional.ofNullable(userPrincipal)
                .map(up -> memberRepository.findByMemberId(up.getMemberId()))
                .orElse(null);


        List<EditorPick> picks = editorPickRepository.findAll();

        List<Artletter> artletters = picks.stream()
                .map(EditorPick::getArtletter)
                .collect(Collectors.toList());

        Map<Long, Boolean> likedMap = artletterLikesRepository.findByMemberAndArtletterIn(member, artletters)
                .stream().collect(Collectors.toMap(al -> al.getArtletter().getLetterId(), al -> true));

        Map<Long, Boolean> scrapedMap = scrapRepository.findByMemberAndArtletterIn(member, artletters)
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
                        .writerSummary( toWriterSummaryDto(artletter.getWriter()) )
                        .category(artletter.getCategory())
                        .readTime(artletter.getReadTime())
                        .thumbnail(artletter.getThumbnail())
                        .likesCnt(likesCountMap.getOrDefault(artletter.getLetterId(), 0))
                        .scrapsCnt(scrapsCountMap.getOrDefault(artletter.getLetterId(), 0))
                        .isLiked(likedMap.getOrDefault(artletter.getLetterId(), false))
                        .isScraped(scrapedMap.getOrDefault(artletter.getLetterId(), false))
                        .createdAt(artletter.getCreatedAt())
                        .updatedAt(artletter.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());


        return artletterList;
    }

    private ArtletterDTO.WriterSummaryDto toWriterSummaryDto(Writer w) {
        if (w == null) return null;
        return ArtletterDTO.WriterSummaryDto.builder()
                .writerId(w.getWriterId())
                .writerName(w.getWriterName())
                .profileImg(w.getProfileImg())
                .writerUrl(w.getWriterUrl())
                .build();
    }


    // 추천바 - 카테고리 조회 api
    @Override
    @Transactional
    public List<String> getRecommendCategory() {

        ArtletterCategory[] allCategories = ArtletterCategory.values();
        List<ArtletterCategory> shuffled = new ArrayList<>(Arrays.asList(allCategories));
        Collections.shuffle(shuffled);

        return shuffled.stream()
                .limit(3)
                .map(Enum::name) // 한글 enum 이름을 문자열로 변환
                .collect(Collectors.toList());
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
    public ResponseEntity<ApiResponse> getScrapArtletters(CustomUserPrincipal userPrincipal, Pageable pageable) {

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
                                    .scrapedAt(scrap.getCreatedAt())
                                    .build();
                        }).toList()
                )).toList();

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, groupedArtletters);
    }

    @Override
    public ResponseEntity<ApiResponse> getScrapCategoryArtletters(CustomUserPrincipal userPrincipal, ArtletterCategory category, Pageable pageable) {

        Member member = memberRepository.findByMemberId(userPrincipal.getMemberId());

        try {
            ArtletterCategory.valueOf(String.valueOf(category));
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
                            .scrapedAt(scrap.getCreatedAt())
                            .build();
                }).toList();

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, artletters);
    }




    /*
    공통 메서드 모음
    */

    private Member getMemberIfAuthenticated(CustomUserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return null;
        }
        return memberRepository.findById(userPrincipal.getMemberId()).orElse(null);
    }

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
        boolean isScraped = member != null && scrapRepository.existsByMemberAndArtletter(member, artletter);
        int scrapCnt = scrapRepository.countByArtletter(artletter);

        return ArtletterDTO.SimpleArtletterResponseDto.builder()
                .artletterId(artletter.getLetterId())
                .title(artletter.getTitle())
                .thumbnail(artletter.getThumbnail())
                .likesCnt(likesCnt)
                .scrapsCnt(scrapCnt)
                .isLiked(isLiked)
                .isScraped(isScraped)
                .updatedAt(artletter.getUpdatedAt())
                .build();
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

        List<ArtletterDTO.CategoryResponseDto> response = artletters.getContent().stream()
                .map(artletter -> {
                    boolean isScraped = (member != null) && scrapRepository.existsByMemberAndArtletter(member, artletter);

                    return ArtletterDTO.CategoryResponseDto.builder()
                            .artletterId(artletter.getLetterId())
                            .title(artletter.getTitle())
                            .thumbnail(artletter.getThumbnail())
                            .tags(artletter.getTag())
                            .isScraped(isScraped)
                            .build();
                }).toList();


        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, response);
    }


    // 현재 아트레터 제외한 랜덤 추천
    public List<ArtletterDTO.CategoryResponseDto> getOtherArtletters(CustomUserPrincipal userPrincipal, Long currentId){
        List<Long> allIds = artletterRepository.findAllIds();

        if (currentId != null) {
            allIds.removeIf(id -> id.equals(currentId));
            // id 있는 경우만 제거하도록 처리
        }

        if (allIds.isEmpty()) {
            return Collections.emptyList();
        }

        Collections.shuffle(allIds);
        List<Long> selectedIds = allIds.stream()
                .limit(3)
                .collect(Collectors.toList());

        List<Artletter> otherArtletters = artletterRepository.findAllById(selectedIds);

        // scrap 했는지 미리 조회 (N+1)
        Member member = (userPrincipal != null) ? memberRepository.findByMemberId(userPrincipal.getMemberId()) : null;
        Map<Long, Boolean> isScrapedMap = (member != null)
                ? scrapRepository.findByMember(member, Pageable.unpaged()).stream()
                .filter(scrap -> scrap.getDeletedAt() == null)
                .collect(Collectors.toMap(scrap -> scrap.getArtletter().getLetterId(), scrap -> true))
                : new HashMap<>();


        return otherArtletters.stream()
                .map(artletter -> ArtletterDTO.CategoryResponseDto.builder()
                        .artletterId(artletter.getLetterId())
                        .title(artletter.getTitle())
                        .thumbnail(artletter.getThumbnail())
                        .tags(artletter.getTag())
                        .isScraped(isScrapedMap.getOrDefault(artletter.getLetterId(), false))
                        .build())
                .collect(Collectors.toList());
    }
}