package com.edison.project.domain.bubble.service;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.response.PageInfo;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.bubble.dto.BubbleRequestDto;
import com.edison.project.domain.bubble.dto.BubbleResponseDto;
import com.edison.project.domain.bubble.entity.Bubble;
import com.edison.project.domain.bubble.entity.BubbleLabel;
import com.edison.project.domain.bubble.repository.BubbleLabelRepository;
import com.edison.project.domain.bubble.repository.BubbleRepository;
import com.edison.project.domain.label.entity.Label;
import com.edison.project.domain.label.repository.LabelRepository;
import com.edison.project.domain.member.entity.Member;
import com.edison.project.domain.member.repository.MemberRepository;
import com.edison.project.global.security.CustomUserPrincipal;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

    @Override
    @Transactional
    public BubbleResponseDto.ListResultDto createBubble(CustomUserPrincipal userPrincipal, BubbleRequestDto.ListDto requestDto) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        // Member 조회
        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // linkedBubble 검증
        Bubble linkedBubble = null;
        if (requestDto.getLinkedBubbleId() != null) {
            linkedBubble = bubbleRepository.findById(requestDto.getLinkedBubbleId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));
        }

        // 라벨 검증
        Set<Long> labelIds = Optional.ofNullable(requestDto.getLabelIds()).orElse(Collections.emptySet());
        if (labelIds.size() > 3) throw new GeneralException(ErrorStatus.LABELS_TOO_MANY);

        Set<Label> labels = new HashSet<>(labelRepository.findAllById(labelIds));
        if (labels.size() != labelIds.size()) throw new GeneralException(ErrorStatus.LABELS_NOT_FOUND);

        // 버블 생성 및 저장
        Bubble savedBubble = bubbleRepository.save(Bubble.builder()
                .member(member)
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .mainImg(requestDto.getMainImageUrl())
                .linkedBubble(linkedBubble)
                .labels(new HashSet<>()) // 초기화
                .build());

        // 라벨과 버블 매핑 후 저장
        Set<BubbleLabel> bubbleLabels = labels.stream()
                .map(label -> BubbleLabel.builder().bubble(savedBubble).label(label).build())
                .collect(Collectors.toSet());

        bubbleLabelRepository.saveAll(bubbleLabels);
        savedBubble.getLabels().addAll(bubbleLabels);

        // ResponseDto 반환
        return BubbleResponseDto.ListResultDto.builder()
                .bubbleId(savedBubble.getBubbleId())
                .title(savedBubble.getTitle())
                .content(savedBubble.getContent())
                .mainImageUrl(savedBubble.getMainImg())
                .labels(labels.stream().map(Label::getName).collect(Collectors.toList()))
                .linkedBubbleId(Optional.ofNullable(linkedBubble).map(Bubble::getBubbleId).orElse(null))
                .createdAt(savedBubble.getCreatedAt())
                .updatedAt(savedBubble.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public BubbleResponseDto.DeleteResultDto deleteBubble(CustomUserPrincipal userPrincipal, Long bubbleId) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        // Bubble 조회
        Bubble bubble = bubbleRepository.findByBubbleIdAndIsDeletedFalse(bubbleId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));

        // 삭제 권한 확인
        if (!bubble.getMember().getMemberId().equals(userPrincipal.getMemberId())) {
        }

        bubble.setDeleted(true);
        bubbleRepository.save(bubble);

        return BubbleResponseDto.DeleteResultDto.builder()
                .bubbleId(bubble.getBubbleId())
                .isDeleted(bubble.isDeleted())
                .build();
    }

    // 버블 복원
    @Override
    @Transactional
    public BubbleResponseDto.RestoreResultDto restoreBubble(CustomUserPrincipal userPrincipal, Long bubbleId) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        // Bubble 조회
        Bubble bubble = bubbleRepository.findByBubbleIdAndIsDeletedTrue(bubbleId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));

        // 복원 권한 확인
        if(!bubble.getMember().getMemberId().equals(userPrincipal.getMemberId())) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        bubble.setDeleted(false);
        bubbleRepository.save(bubble);

        return BubbleResponseDto.RestoreResultDto.builder()
                .bubbleId(bubble.getBubbleId())
                .isRestored(!bubble.isDeleted())
                .build();
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getBubblesByMember(CustomUserPrincipal userPrincipal, Pageable pageable) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        Page<Bubble> bubblePage = bubbleRepository.findByMember_MemberIdAndIsDeletedFalse(userPrincipal.getMemberId(), pageable);

        PageInfo pageInfo = new PageInfo(bubblePage.getNumber(), bubblePage.getSize(), bubblePage.hasNext(),
                bubblePage.getTotalElements(), bubblePage.getTotalPages());

        // Bubble 데이터 변환
        List<BubbleResponseDto.ListResultDto> bubbles = bubblePage.getContent().stream()
                .map(bubble -> BubbleResponseDto.ListResultDto.builder()
                        .bubbleId(bubble.getBubbleId())
                        .title(bubble.getTitle())
                        .content(bubble.getContent())
                        .mainImageUrl(bubble.getMainImg())
                        .labels(bubble.getLabels().stream()
                                .map(label -> label.getLabel().getName())
                                .collect(Collectors.toList()))
                        .linkedBubbleId(Optional.ofNullable(bubble.getLinkedBubble())
                                .map(Bubble::getBubbleId)
                                .orElse(null))
                        .createdAt(bubble.getCreatedAt())
                        .updatedAt(bubble.getUpdatedAt())
                        .build())
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
        List<BubbleResponseDto.ListResultDto> bubbles = bubblePage.getContent().stream()
                .map(bubble -> BubbleResponseDto.ListResultDto.builder()
                        .bubbleId(bubble.getBubbleId())
                        .title(bubble.getTitle())
                        .content(bubble.getContent())
                        .mainImageUrl(bubble.getMainImg())
                        .labels(bubble.getLabels().stream()
                                .map(label -> label.getLabel().getName())
                                .collect(Collectors.toList()))
                        .linkedBubbleId(Optional.ofNullable(bubble.getLinkedBubble())
                                .map(Bubble::getBubbleId)
                                .orElse(null))
                        .createdAt(bubble.getCreatedAt())
                        .updatedAt(bubble.getUpdatedAt())
                        .build())
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
    public BubbleResponseDto.ListResultDto getBubble(CustomUserPrincipal userPrincipal, Long bubbleId) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        Bubble bubble = bubbleRepository.findByBubbleIdAndIsDeletedFalse(bubbleId).orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));

        // 조회 권한 확인
        if (!bubble.getMember().getMemberId().equals(userPrincipal.getMemberId())) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        return BubbleResponseDto.ListResultDto.builder()
                .bubbleId(bubble.getBubbleId())
                .title(bubble.getTitle())
                .content(bubble.getContent())
                .mainImageUrl(bubble.getMainImg())
                .labels(bubble.getLabels().stream()
                        .map(label -> label.getLabel().getName())
                        .collect(Collectors.toList()))
                .linkedBubbleId(Optional.ofNullable(bubble.getLinkedBubble())
                        .map(Bubble::getBubbleId)
                        .orElse(null))
                .createdAt(bubble.getCreatedAt())
                .updatedAt(bubble.getUpdatedAt())
                .build();
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

        List<BubbleResponseDto.ListResultDto> results = paginatedBubbles.stream()
                .map(bubble -> BubbleResponseDto.ListResultDto.builder()
                        .bubbleId(bubble.getBubbleId())
                        .title(bubble.getTitle())
                        .content(bubble.getContent())
                        .mainImageUrl(bubble.getMainImg())
                        .labels(bubble.getLabels().stream()
                                .map(bl -> bl.getLabel().getName())
                                .collect(Collectors.toList()))
                        .linkedBubbleId(Optional.ofNullable(bubble.getLinkedBubble())
                                .map(Bubble::getBubbleId)
                                .orElse(null))
                        .createdAt(bubble.getCreatedAt())
                        .updatedAt(bubble.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, results);
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
