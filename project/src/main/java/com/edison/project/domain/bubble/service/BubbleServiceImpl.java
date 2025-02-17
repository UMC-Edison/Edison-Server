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
            return Collections.emptySet(); // 빈 Set 반환
        }
        if (idxs.size() > 3) {
            throw new GeneralException(ErrorStatus.LABELS_TOO_MANY);
        }

        Set<Label> labels = new HashSet<>(labelRepository.findAllByMemberAndLocalIdxIn(member, idxs));
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
}
