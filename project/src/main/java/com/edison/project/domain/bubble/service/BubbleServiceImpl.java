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
        List<LabelResponseDTO.CreateResultDto> labelDtos = bubble.getLabels().stream()
                .map(bl -> LabelResponseDTO.CreateResultDto.builder()
                        .labelId(bl.getLabel().getLabelId())
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
                .linkedBubbleId(Optional.ofNullable(bubble.getLinkedBubble())
                        .map(Bubble::getBubbleId)
                        .orElse(null))
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
                    List<LabelResponseDTO.CreateResultDto> labelDtos = bubble.getLabels().stream()
                            .map(bl -> LabelResponseDTO.CreateResultDto.builder()
                                    .labelId(bl.getLabel().getLabelId())
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
                            .linkedBubbleId(Optional.ofNullable(bubble.getLinkedBubble())
                                    .map(Bubble::getBubbleId)
                                    .orElse(null))
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

            bubbleIds.forEach(bubbleRepository::clearLinkedBubble);

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

        // linkedBubble 검증
        Bubble linkedBubble = null;
        if (request.getLinkedBubbleId() != null) {
            linkedBubble = bubbleRepository.findById(request.getLinkedBubbleId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.LINKEDBUBBLE_NOT_FOUND));
        }

        // backlink 검증
        Set<Bubble> backlinks = new HashSet<>();
        if (request.getBacklinkIds() != null) {
            backlinks = new HashSet<>(bubbleRepository.findAllById(request.getBacklinkIds()));
            if (backlinks.size() != request.getBacklinkIds().size()) {
                throw new GeneralException(ErrorStatus.BACKLINK_NOT_FOUND);
            }
        }

        // 라벨 검증
        Set<Long> labelIds = Optional.ofNullable(request.getLabelIds()).orElse(Collections.emptySet());
        if (labelIds.size() > 3) throw new GeneralException(ErrorStatus.LABELS_TOO_MANY);

        Set<Label> labels = new HashSet<>(labelRepository.findAllById(labelIds));
        if (labels.size() != labelIds.size()) throw new GeneralException(ErrorStatus.LABELS_NOT_FOUND);

        Bubble bubble;


        if (request.isDeleted()) {
            bubble = bubbleRepository.existsById(request.getBubbleId()) ?
                    hardDeleteBubble(request, member) : null;
        } else {
            bubble =  bubbleRepository.existsById(request.getBubbleId()) ?
                    updateExistingBubble(request, member, linkedBubble, backlinks, labels)
                    : createNewBubble(request, member, linkedBubble, backlinks, labels);
        }

        if (!request.isDeleted()) {
            // 결과 반환
            List<LabelResponseDTO.CreateResultDto> labelDtos = bubble.getLabels().stream()
                    .map(bl -> LabelResponseDTO.CreateResultDto.builder()
                            .labelId(bl.getLabel().getLabelId())
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
                    .linkedBubbleId(Optional.ofNullable(bubble.getLinkedBubble())
                            .map(Bubble::getBubbleId)
                            .orElse(null))
                    .backlinkIds(bubble.getBacklinks().stream()
                            .map(BubbleBacklink::getBacklinkBubble)
                            .map(Bubble::getBubbleId)
                            .collect(Collectors.toSet()))
                    .isDeleted(request.isDeleted())
                    .isTrashed(bubble.isTrashed())
                    .createdAt(bubble.getCreatedAt())
                    .updatedAt(bubble.getUpdatedAt())
                    .deletedAt(bubble.getDeletedAt())
                    .build();
        } else {
            return BubbleResponseDto.SyncResultDto.builder()
                    .bubbleId(request.getBubbleId())
                    .title(null)
                    .content(null)
                    .mainImageUrl(null)
                    .labels(null)
                    .linkedBubbleId(null)
                    .backlinkIds(null)
                    .isDeleted(request.isDeleted())
                    .isTrashed(null)
                    .createdAt(null)
                    .updatedAt(null)
                    .deletedAt(null)
                    .build();
        }
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

    private Bubble updateExistingBubble(BubbleRequestDto.SyncDto request, Member member, Bubble linkedBubble, Set<Bubble> backlinks, Set<Label> labels) {
        Bubble bubble = bubbleRepository.findById(request.getBubbleId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));

        if(!bubble.getMember().getMemberId().equals(member.getMemberId())) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        Set<BubbleLabel> bubbleLabels = labels.stream()
                .map(label -> BubbleLabel.builder().bubble(bubble).label(label).build())
                .collect(Collectors.toSet());

        bubble.update(request.getTitle(), request.getContent(), request.getMainImageUrl(), linkedBubble, bubbleLabels);

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

        bubbleRepository.saveAndFlush(bubble);
        return bubble;
    }

    private Bubble hardDeleteBubble(BubbleRequestDto.SyncDto request, Member member) {

        Bubble bubble = bubbleRepository.findById(request.getBubbleId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));

        // 권한 확인
        if (!bubble.getMember().getMemberId().equals(member.getMemberId())) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        // linkedBubble = bubbleId인 모든 버블의 linkedBubble 값을 null로 업데이트
        bubbleRepository.clearLinkedBubble(request.getBubbleId());

        bubbleRepository.delete(bubble);
        return null;
    }

    private Bubble createNewBubble(BubbleRequestDto.SyncDto request, Member member, Bubble linkedBubble, Set<Bubble> backlinks, Set<Label> labels) {
        Bubble newBubble = Bubble.builder()
                .bubbleId(request.getBubbleId())
                .title(request.getTitle())
                .content(request.getContent())
                .mainImg(request.getMainImageUrl())
                .linkedBubble(linkedBubble)
                .member(member)
                .labels(new HashSet<>())
                .isTrashed(request.isTrashed())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .deletedAt(request.getDeletedAt())
                .build();

        Bubble savedBubble = bubbleRepository.save(newBubble);

        Set<BubbleBacklink> newbacklinks = backlinks.stream()
                .map(backlink -> BubbleBacklink.builder()
                        .bubble(savedBubble)
                        .backlinkBubble(backlink)
                        .build())
                .collect(Collectors.toSet());
        savedBubble.getBacklinks().addAll(newbacklinks);

        Set<BubbleLabel> bubbleLabels = labels.stream()
                .map(label -> BubbleLabel.builder().bubble(savedBubble).label(label).build())
                .collect(Collectors.toSet());

        bubbleLabelRepository.saveAll(bubbleLabels);

        savedBubble.getLabels().addAll(bubbleLabels);

        return savedBubble;
    }
}
