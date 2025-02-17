package com.edison.project.domain.bubble.service;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.response.PageInfo;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.bubble.dto.BubbleRequestDto;
import com.edison.project.domain.bubble.dto.BubbleResponseDto;
import com.edison.project.domain.bubble.entity.Bubble;
import com.edison.project.domain.bubble.entity.BubbleBacklink;
import com.edison.project.domain.bubble.entity.BubbleLabel;
import com.edison.project.domain.bubble.repository.BubbleLabelRepository;
import com.edison.project.domain.bubble.repository.BubbleRepository;
import com.edison.project.domain.label.dto.LabelResponseDTO;
import com.edison.project.domain.label.entity.Label;
import com.edison.project.domain.label.repository.LabelRepository;
import com.edison.project.domain.member.entity.Member;
import com.edison.project.domain.member.repository.MemberRepository;
import com.edison.project.global.security.CustomUserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BubbleServiceImpl implements BubbleService {

    private final BubbleRepository bubbleRepository;
    private final BubbleLabelRepository bubbleLabelRepository;
    private final LabelRepository labelRepository;
    private final MemberRepository memberRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // Bubble -> BubbleResponseDto 변환 메서드 (공통 로직)
    private BubbleResponseDto.SyncResultDto convertToBubbleResponseDto(Bubble bubble) {
        List<LabelResponseDTO.LabelSimpleInfoDto> labelDtos = bubble.getLabels().stream()
                .map(bl -> LabelResponseDTO.LabelSimpleInfoDto.builder()
                        .localIdx(bl.getLabel().getLocalIdx())
                        .name(bl.getLabel().getName())
                        .color(bl.getLabel().getColor())
                        .build())
                .collect(Collectors.toList());

        return BubbleResponseDto.SyncResultDto.builder()
                .bubbleId(bubble.getBubbleId())
                .title(bubble.getTitle())
                .content(bubble.getContent())
                .mainImageUrl(bubble.getMainImg())
                .labels(labelDtos)
                .backlinkIds(bubble.getBacklinks().stream()
                        .map(BubbleBacklink::getBacklinkBubble)
                        .map(Bubble::getBubbleId)
                        .collect(Collectors.toSet()))
                .isTrashed(bubble.isTrashed())
                .createdAt(bubble.getCreatedAt())
                .updatedAt(bubble.getUpdatedAt())
                .deletedAt(bubble.getDeletedAt())
                .build();
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getBubblesByMember(CustomUserPrincipal userPrincipal, Pageable pageable) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        Page<Bubble> bubblePage = bubbleRepository.findByMember_MemberIdAndIsTrashedFalse(userPrincipal.getMemberId(), pageable);

        PageInfo pageInfo = new PageInfo(bubblePage.getNumber(), bubblePage.getSize(), bubblePage.hasNext(),
                bubblePage.getTotalElements(), bubblePage.getTotalPages());

        // Bubble 데이터 변환
        List<BubbleResponseDto.SyncResultDto> bubbles = bubblePage.getContent().stream()
                .map(this::convertToBubbleResponseDto)
                .collect(Collectors.toList());

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, bubbles);
    }

    @Override
    public ResponseEntity<ApiResponse> getDeletedBubbles(CustomUserPrincipal userPrincipal, Pageable pageable) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        // Member 조회
        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Page<Bubble> bubblePage = bubbleRepository.findByMember_MemberIdAndIsTrashedTrue(userPrincipal.getMemberId(), pageable);

        PageInfo pageInfo = new PageInfo(bubblePage.getNumber(), bubblePage.getSize(), bubblePage.hasNext(),
                bubblePage.getTotalElements(), bubblePage.getTotalPages());

        // Bubble 데이터 변환
        // Bubble -> DeletedListResultDto 변환
        List<BubbleResponseDto.TrashedListResultDto> bubbles = bubblePage.getContent().stream()
                .map(bubble -> {
                    LocalDateTime updatedAt = bubble.getUpdatedAt();
                    LocalDateTime now = LocalDateTime.now();
                    long remainDays = 30 - ChronoUnit.DAYS.between(updatedAt, now);

                    // 라벨 정보 변환
                    List<LabelResponseDTO.LabelSimpleInfoDto> labelDtos = bubble.getLabels().stream()
                            .map(bl -> LabelResponseDTO.LabelSimpleInfoDto.builder()
                                    .localIdx(bl.getLabel().getLocalIdx())
                                    .name(bl.getLabel().getName())
                                    .color(bl.getLabel().getColor())
                                    .build())
                            .collect(Collectors.toList());

                    return BubbleResponseDto.TrashedListResultDto.builder()
                            .bubbleId(bubble.getBubbleId())
                            .title(bubble.getTitle())
                            .content(bubble.getContent())
                            .mainImageUrl(bubble.getMainImg())
                            .labels(labelDtos) // 라벨 정보 추가
                            .backlinkIds(bubble.getBacklinks().stream()
                                    .map(BubbleBacklink::getBacklinkBubble)
                                    .map(Bubble::getBubbleId)
                                    .collect(Collectors.toSet()))
                            .createdAt(bubble.getCreatedAt())
                            .updatedAt(updatedAt)
                            .remainDay((int) Math.max(remainDays, 0)) // 남은 일수 계산
                            .build();
                })
                .collect(Collectors.toList());

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, bubbles);
    }
  
  @Override
  public ResponseEntity<ApiResponse> getRecentBubblesByMember(CustomUserPrincipal userPrincipal, Pageable pageable) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }
  
        LocalDateTime sevenDaysago = LocalDateTime.now().minusDays(7);

        // 7일 이내 버블 조회
        Page<Bubble> bubblePage = bubbleRepository.findRecentBubblesByMember(userPrincipal.getMemberId(), sevenDaysago, pageable);

        // DTO로 변환
        List<BubbleResponseDto.SyncResultDto> bubbles = bubblePage.getContent().stream()
                .map(this::convertToBubbleResponseDto)
                .collect(Collectors.toList());

        PageInfo pageInfo = new PageInfo(
                bubblePage.getNumber(),
                bubblePage.getSize(),
                bubblePage.hasNext(),
                bubblePage.getTotalElements(),
                bubblePage.getTotalPages()
        );

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, bubbles);
    }


    @Override
    public BubbleResponseDto.SyncResultDto getBubble(CustomUserPrincipal userPrincipal, Long bubbleId) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        Bubble bubble = bubbleRepository.findByBubbleIdAndIsTrashedFalse(bubbleId).orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));

        // 조회 권한 확인
        if (!bubble.getMember().getMemberId().equals(userPrincipal.getMemberId())) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        return convertToBubbleResponseDto(bubble);
    }

    // 버블 검색
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> searchBubbles(CustomUserPrincipal userPrincipal, String keyword, boolean recent, Pageable pageable) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }
        memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        List<Bubble> bubbles = bubbleRepository.searchBubblesByKeyword(keyword);

        // 7일 이내 필터링 조건
        if (Boolean.TRUE.equals(recent)) {
            ZonedDateTime sevenDaysAgoZoned = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(7);
            LocalDateTime sevenDaysAgo = sevenDaysAgoZoned.toLocalDateTime();

            bubbles = bubbles.stream()
                    .filter(bubble -> bubble.getUpdatedAt().isAfter(sevenDaysAgo))
                    .collect(Collectors.toList());
        }


        // 검색어 정렬 : 제목, 본문, 오래된 순서 순
        List<Bubble> sortedBubbles = bubbles.stream()
                .sorted((b1, b2) -> {
                    boolean b1TitleMatch = b1.getTitle().contains(keyword);
                    boolean b2TitleMatch = b2.getTitle().contains(keyword);
                    if (b1TitleMatch && !b2TitleMatch) return -1;
                    if (!b1TitleMatch && b2TitleMatch) return 1;

                    int b1ContentMatchCount = countOccurrences(b1.getContent(), keyword);
                    int b2ContentMatchCount = countOccurrences(b2.getContent(), keyword);
                    if (b1ContentMatchCount != b2ContentMatchCount) {
                        return Integer.compare(b2ContentMatchCount, b1ContentMatchCount);
                    }

                    return b1.getUpdatedAt().compareTo(b2.getUpdatedAt());
                })
                .collect(Collectors.toList());

        // 페이징 적용
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedBubbles.size());
        List<Bubble> paginatedBubbles = sortedBubbles.subList(start, end);

        PageInfo pageInfo = new PageInfo(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                end < sortedBubbles.size(),
                (long) sortedBubbles.size(),
                (sortedBubbles.size() + pageable.getPageSize() - 1) / pageable.getPageSize()
        );

        List<BubbleResponseDto.SyncResultDto> results = paginatedBubbles.stream()
                .map(this::convertToBubbleResponseDto)
                .collect(Collectors.toList());

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, results);
    }

    @Override
    // @Scheduled(cron = "0 0 0 * * ?") // 매일 새벽 0시에 실행
    @Scheduled(cron = "0 35 17 * * ?", zone = "Asia/Seoul") // 테스트할 시간
    @Transactional
    public void deleteExpiredBubble() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.minusDays(30);

        List<Bubble> expiredBubbles = bubbleRepository.findAllByUpdatedAtBeforeAndIsTrashedTrue(expiryDate);

        if (!expiredBubbles.isEmpty()) {
            List<Long> bubbleIds = expiredBubbles.stream()
                            .map(Bubble::getBubbleId)
                                    .collect(Collectors.toList());

            bubbleRepository.deleteAll(expiredBubbles);
            log.info("Deleted {} expired bubbles", expiredBubbles.size());
        } else {
            log.info("No expired bubbles found for deletion");
        }

    }

    @Override
    @Transactional
    public BubbleResponseDto.SyncResultDto syncBubble(CustomUserPrincipal userPrincipal, BubbleRequestDto.SyncDto request) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Set<Bubble> backlinks = validateBacklinks(request.getBacklinkIds(), member);
        Set<Label> labels = validateLabels(request.getLabelIdxs(), member);  // idx -> label Pk

        Bubble bubble = processBubble(request, member, backlinks, labels);
        return buildSyncResultDto(request, bubble, member);
    }

    private Bubble processBubble(BubbleRequestDto.SyncDto request, Member member, Set<Bubble> backlinks, Set<Label> labels) {
        if (request.isDeleted()){
            return bubbleRepository.existsById(request.getBubbleId()) ? hardDeleteBubble(request, member) : null;
        }

        return bubbleRepository.existsById(request.getBubbleId())
                ? updateExistingBubble(request, member, backlinks, labels)
                : createNewBubble(request, member, backlinks, labels);
    }

    private Bubble updateExistingBubble(BubbleRequestDto.SyncDto request, Member member, Set<Bubble> backlinks, Set<Label> labels) {
        Bubble bubble = bubbleRepository.findById(request.getBubbleId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));

        if(!bubble.getMember().getMemberId().equals(member.getMemberId())) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        Set<BubbleLabel> bubbleLabels = labels.stream()
                .map(label -> BubbleLabel.builder().bubble(bubble).label(label).build())
                .collect(Collectors.toSet());
        bubble.update(request.getTitle(), request.getContent(), request.getMainImageUrl(), bubbleLabels);

        bubble.getBacklinks().clear();
        Set<BubbleBacklink> newbacklinks = backlinks.stream()
                .map(backlink -> BubbleBacklink.builder()
                        .bubble(bubble)
                        .backlinkBubble(backlink)
                        .build())
                .collect(Collectors.toSet());
        bubble.getBacklinks().addAll(newbacklinks);

        bubble.setTrashed(request.isTrashed());
        bubble.setUpdatedAt(request.getUpdatedAt());
        bubble.setDeletedAt(request.getDeletedAt());

        return bubbleRepository.saveAndFlush(bubble);
    }

    private Bubble hardDeleteBubble(BubbleRequestDto.SyncDto request, Member member) {

        Bubble bubble = bubbleRepository.findById(request.getBubbleId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));

        if (!bubble.getMember().getMemberId().equals(member.getMemberId())) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        bubbleRepository.delete(bubble);
        return null;
    }

    private Bubble createNewBubble(BubbleRequestDto.SyncDto request, Member member, Set<Bubble> backlinks, Set<Label> labels) {
        Bubble newBubble = Bubble.builder()
                .bubbleId(request.getBubbleId())
                .title(request.getTitle())
                .content(request.getContent())
                .mainImg(request.getMainImageUrl())
                .member(member)
                .labels(new HashSet<>())
                .isTrashed(request.isTrashed())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .deletedAt(request.getDeletedAt())
                .build();

        Bubble savedBubble = bubbleRepository.save(newBubble);

        Set<BubbleLabel> bubbleLabels = labels.stream()
                .map(label -> BubbleLabel.builder().bubble(savedBubble).label(label).build())
                .collect(Collectors.toSet());

        bubbleLabelRepository.saveAll(bubbleLabels); // DB에 저장
        savedBubble.getLabels().addAll(bubbleLabels); // Bubble에 추가
        bubbleRepository.saveAndFlush(savedBubble); // 즉시 반영

        Set<BubbleBacklink> newbacklinks = backlinks.stream()
                .map(backlink -> BubbleBacklink.builder()
                        .bubble(savedBubble)
                        .backlinkBubble(backlink)
                        .build())
                .collect(Collectors.toSet());
        savedBubble.getBacklinks().addAll(newbacklinks);

        return savedBubble;
    }

    // SyncResultDto 생성
    private BubbleResponseDto.SyncResultDto buildSyncResultDto(BubbleRequestDto.SyncDto request, Bubble bubble, Member member) {
        if (bubble == null) {
            return BubbleResponseDto.SyncResultDto.builder()
                    .bubbleId(request.getBubbleId())
                    .title(request.getTitle())
                    .content(request.getContent())
                    .mainImageUrl(request.getMainImageUrl())
                    .labels(mapLabelsToDtoByLocalIdx(member, request.getLabelIdxs()))
                    .backlinkIds(request.getBacklinkIds())
                    .isDeleted(true)
                    .isTrashed(request.isTrashed())
                    .createdAt(request.getCreatedAt())
                    .updatedAt(request.getUpdatedAt())
                    .deletedAt(request.getDeletedAt())
                    .build();
        }
        Set<Label> labels = bubble.getLabels().stream()
                .map(BubbleLabel::getLabel)
                .collect(Collectors.toSet());

        return BubbleResponseDto.SyncResultDto.builder()
                .bubbleId(bubble.getBubbleId())
                .title(bubble.getTitle())
                .content(bubble.getContent())
                .mainImageUrl(bubble.getMainImg())
                .labels(mapLabelsToDto(labels))
                .backlinkIds(bubble.getBacklinks().stream()
                        .map(BubbleBacklink::getBacklinkBubble)
                        .map(Bubble::getBubbleId)
                        .collect(Collectors.toSet()))
                .isDeleted(false)
                .isTrashed(bubble.isTrashed())
                .createdAt(bubble.getCreatedAt())
                .updatedAt(bubble.getUpdatedAt())
                .deletedAt(bubble.getDeletedAt())
                .build();
    }

    // 백링크 검증
    private Set<Bubble> validateBacklinks(Set<Long> backlinkIds, Member member) {
        if (backlinkIds == null || backlinkIds.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Bubble> backlinks = new HashSet<>(bubbleRepository.findAllById(backlinkIds));
        if (backlinks.size() != backlinkIds.size()) {
            throw new GeneralException(ErrorStatus.BACKLINK_NOT_FOUND);
        }
        if (!backlinks.stream().allMatch(backlink-> backlink.getMember().equals(member))) {
            throw new GeneralException(ErrorStatus.BACKLINK_FORBIDDEN);
        }
        return backlinks;
    }

    private Set<Label> validateLabels(Set<Long> labelIdxs, Member member) {
        Set<Long> idxs = Optional.ofNullable(labelIdxs).orElse(Collections.emptySet());

        if (idxs.isEmpty()) {
            System.out.println("🔴 LabelIdxs가 비어 있음");
            return Collections.emptySet(); // 빈 Set 반환
        }

        if (idxs.size() > 3) {
            throw new GeneralException(ErrorStatus.LABELS_TOO_MANY);
        }

        System.out.println("🟢 찾으려는 labelIdxs: " + idxs);
        System.out.println("🟢 찾으려는 member: " + member.getMemberId());

        Set<Label> labels = new HashSet<>(labelRepository.findAllByMemberAndLocalIdxIn(member, idxs));

        if (labels.isEmpty()) {
            System.out.println("🔴 반환된 Label이 없음");
        } else {
            System.out.println("🟢 반환된 Labels: " + labels);
        }

        // 조회된 라벨의 localIdx와 요청된 localIdx가 일치하는지 확인
        Set<Long> foundIdxs = labels.stream().map(Label::getLocalIdx).collect(Collectors.toSet());
        if (!foundIdxs.containsAll(idxs)) {
            throw new GeneralException(ErrorStatus.LABELS_NOT_FOUND);
        }

        if (!labels.stream().allMatch(label -> label.getMember().equals(member))) {
            throw new GeneralException(ErrorStatus.LABELS_FORBIDDEN);
        }

        return new HashSet<>(labels);
    }


    // 라벨을 DTO로 변환 (localIdx 기준)
    public List<LabelResponseDTO.LabelSimpleInfoDto> mapLabelsToDtoByLocalIdx(Member member, Set<Long> localIdxs) {
        return localIdxs.stream()
                .map(localIdx -> labelRepository.findLabelByMemberAndLocalIdx(member, localIdx)
                        .orElseThrow(() -> new GeneralException(ErrorStatus.LABELS_NOT_FOUND)))
                .map(l -> LabelResponseDTO.LabelSimpleInfoDto.builder()
                        .localIdx(l.getLocalIdx()) // labelId 대신 localIdx 사용
                        .name(l.getName())
                        .color(l.getColor())
                        .build())
                .collect(Collectors.toList());
    }

    // 라벨을 DTO로 변환
    public List<LabelResponseDTO.LabelSimpleInfoDto> mapLabelsToDto(Set<Label> labels) {
        return labels.stream()
                .map(l -> LabelResponseDTO.LabelSimpleInfoDto.builder()
                        .localIdx(l.getLocalIdx())
                        .name(l.getName())
                        .color(l.getColor())
                        .build())
                .collect(Collectors.toList());
    }

    private int countOccurrences(String content, String keyword) {
        if (content == null || keyword == null || keyword.isEmpty()) return 0;
        int count = 0;
        int idx = content.indexOf(keyword);
        while (idx != -1) {
            count++;
            idx = content.indexOf(keyword, idx + keyword.length());
        }
        return count;
    }
}
