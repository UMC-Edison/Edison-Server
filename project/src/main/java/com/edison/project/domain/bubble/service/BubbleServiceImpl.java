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
import com.edison.project.domain.bubble.repository.BubbleBacklinkRepository;
import com.edison.project.domain.bubble.repository.BubbleLabelRepository;
import com.edison.project.domain.bubble.repository.BubbleRepository;
import com.edison.project.domain.label.dto.LabelResponseDTO;
import com.edison.project.domain.label.entity.Label;
import com.edison.project.domain.label.repository.LabelRepository;
import com.edison.project.domain.member.entity.Member;
import com.edison.project.domain.member.repository.MemberRepository;
import com.edison.project.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BubbleServiceImpl implements BubbleService {

    private final BubbleRepository bubbleRepository;
    private final BubbleLabelRepository bubbleLabelRepository;
    private final LabelRepository labelRepository;
    private final MemberRepository memberRepository;
    private final BubbleBacklinkRepository bubbleBacklinkRepository;

    // Bubble → DTO 변환
    private BubbleResponseDto.SyncResultDto convertToBubbleResponseDto(Bubble bubble) {
        return BubbleResponseDto.SyncResultDto.builder()
                .localIdx(bubble.getLocalIdx())
                .title(bubble.getTitle())
                .content(bubble.getContent())
                .mainImageUrl(bubble.getMainImg())
                .labels(mapLabelsToDto(bubble.getLabels().stream()
                        .map(BubbleLabel::getLabel)
                        .collect(Collectors.toSet())))
                .backlinkIdxs(bubble.getBacklinks().stream()
                        .map(BubbleBacklink::getBacklinkBubble)
                        .map(Bubble::getLocalIdx)
                        .collect(Collectors.toSet()))
                .isTrashed(bubble.isTrashed())
                .createdAt(bubble.getCreatedAt())
                .updatedAt(bubble.getUpdatedAt())
                .deletedAt(bubble.getDeletedAt())
                .build();
    }

    /** 전체 버블 목록 조회 */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getBubblesByMember(CustomUserPrincipal userPrincipal, Pageable pageable) {

        // Fetch Join으로 한 번에 조회
        List<Bubble> allBubbles = bubbleRepository.findByMemberWithDetails(userPrincipal.getMemberId());

        // 메모리에서 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allBubbles.size());
        List<Bubble> pagedBubbles = start < allBubbles.size()
                ? allBubbles.subList(start, end)
                : Collections.emptyList();

        PageInfo pageInfo = new PageInfo(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                end < allBubbles.size(),
                (long) allBubbles.size(),
                (int) Math.ceil((double) allBubbles.size() / pageable.getPageSize())
        );

        List<BubbleResponseDto.SyncResultDto> bubbles = pagedBubbles.stream()
                .map(this::convertToBubbleResponseDto)
                .collect(Collectors.toList());

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, bubbles);
    }


    /** 휴지통 버블 목록 조회 */
    @Override
    @Transactional(readOnly = true)  // 추가
    public ResponseEntity<ApiResponse> getDeletedBubbles(CustomUserPrincipal userPrincipal, Pageable pageable) {

        List<Bubble> allBubbles = bubbleRepository.findTrashedByMemberWithDetails(userPrincipal.getMemberId());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allBubbles.size());
        List<Bubble> pagedBubbles = start < allBubbles.size()
                ? allBubbles.subList(start, end)
                : Collections.emptyList();

        PageInfo pageInfo = new PageInfo(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                end < allBubbles.size(),
                (long) allBubbles.size(),
                (int) Math.ceil((double) allBubbles.size() / pageable.getPageSize())
        );

        LocalDateTime now = LocalDateTime.now();  // 루프 밖으로 이동

        List<BubbleResponseDto.TrashedListResultDto> bubbles = pagedBubbles.stream()
                .map(bubble -> {
                    LocalDateTime deletedAt = Optional.ofNullable(bubble.getDeletedAt()).orElse(now);
                    long remainDays = 30 - ChronoUnit.DAYS.between(deletedAt, now);

                    List<LabelResponseDTO.LabelSimpleInfoDto> labelDtos = bubble.getLabels().stream()
                            .map(bl -> LabelResponseDTO.LabelSimpleInfoDto.builder()
                                    .localIdx(bl.getLabel().getLocalIdx())
                                    .name(bl.getLabel().getName())
                                    .color(bl.getLabel().getColor())
                                    .build())
                            .collect(Collectors.toList());

                    return BubbleResponseDto.TrashedListResultDto.builder()
                            .localIdx(bubble.getLocalIdx())
                            .title(bubble.getTitle())
                            .content(bubble.getContent())
                            .mainImageUrl(bubble.getMainImg())
                            .labels(labelDtos)
                            .backlinkIdxs(bubble.getBacklinks().stream()
                                    .map(BubbleBacklink::getBacklinkBubble)
                                    .map(Bubble::getLocalIdx)
                                    .collect(Collectors.toSet()))
                            .createdAt(bubble.getCreatedAt())
                            .updatedAt(bubble.getUpdatedAt())
                            .deletedAt(bubble.getDeletedAt())
                            .remainDay((int) Math.max(remainDays, 0))
                            .build();
                })
                .collect(Collectors.toList());

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, bubbles);
    }

    /** 최근 7일 내 버블 조회 */
    @Override
    @Transactional(readOnly = true)  // 추가
    public ResponseEntity<ApiResponse> getRecentBubblesByMember(CustomUserPrincipal userPrincipal, Pageable pageable) {

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        List<Bubble> allBubbles = bubbleRepository.findRecentByMemberWithDetails(
                userPrincipal.getMemberId(), sevenDaysAgo);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allBubbles.size());
        List<Bubble> pagedBubbles = start < allBubbles.size()
                ? allBubbles.subList(start, end)
                : Collections.emptyList();

        List<BubbleResponseDto.SyncResultDto> bubbles = pagedBubbles.stream()
                .map(this::convertToBubbleResponseDto)
                .collect(Collectors.toList());

        PageInfo pageInfo = new PageInfo(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                end < allBubbles.size(),
                (long) allBubbles.size(),
                (int) Math.ceil((double) allBubbles.size() / pageable.getPageSize())
        );

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, bubbles);
    }

    /** 버블 상세 조회 */
    @Override
    @Transactional(readOnly = true)
    public BubbleResponseDto.SyncResultDto getBubble(CustomUserPrincipal userPrincipal, String localIdx) {

        Bubble bubble = bubbleRepository.findByMemberAndLocalIdxWithDetails(
                        userPrincipal.getMemberId(), localIdx)
                .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));

        return convertToBubbleResponseDto(bubble);
    }


    /** 버블 SYNC */
    @Override
    @Transactional
    public BubbleResponseDto.SyncResultDto syncBubble(CustomUserPrincipal userPrincipal, BubbleRequestDto.SyncDto request) {

        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // idx -> label Pk
        Set<Bubble> backlinks = validateBacklinks(request.getBacklinkIds(), member);
        Set<Label> labels = validateLabels(request.getLabelIdxs(), member);

        Bubble bubble = processBubble(request, member, backlinks, labels);
        return buildSyncResultDto(request, bubble, member);
    }

    private Bubble processBubble(BubbleRequestDto.SyncDto request, Member member, Set<Bubble> backlinks, Set<Label> labels) {
        if (request.isDeleted()){
            return bubbleRepository.existsByMemberAndLocalIdx(member, request.getLocalIdx()) ? hardDeleteBubble(request, member) : null;
        }

        return bubbleRepository.existsByMemberAndLocalIdx(member, request.getLocalIdx())
                ? updateExistingBubble(request, member, backlinks, labels)
                : createNewBubble(request, member, backlinks, labels);
    }

    private Bubble updateExistingBubble(BubbleRequestDto.SyncDto request, Member member, Set<Bubble> backlinks, Set<Label> labels) {
        Bubble bubble = bubbleRepository.findByMemberAndLocalIdx(member, request.getLocalIdx())
                .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));

        // 관련된 백링크들 가져오기
        List<BubbleBacklink> backlinksFromBubble = bubbleBacklinkRepository.findByBubble_BubbleId(bubble.getBubbleId());
        //이 버블을 백링크로 걸은 백링크목록
        List<BubbleBacklink> backlinksToBubble = bubbleBacklinkRepository.findByBacklinkBubble_BubbleId(bubble.getBubbleId());

        System.out.println("▶▶▶ 내가 다른 버블을 가리키는 백링크 목록 (A → B):");
        for (BubbleBacklink backlink : backlinksFromBubble) {
            System.out.println("A → B: 내 ID = " + backlink.getBubble().getBubbleId()
                    + " → 대상 ID = " + backlink.getBacklinkBubble().getBubbleId());
        }

        System.out.println("▶▶▶ 나를 가리키는 백링크 목록 (B → A):");
        for (BubbleBacklink backlink : backlinksToBubble) {
            System.out.println("B → A: 상대 ID = " + backlink.getBubble().getBubbleId()
                    + " → 나 ID = " + backlink.getBacklinkBubble().getBubbleId());
        }

        Set<BubbleBacklink> allBacklinks = new HashSet<>();
        allBacklinks.addAll(backlinksFromBubble);
        allBacklinks.addAll(backlinksToBubble);

        // 하드 딜리트 처리
        if (request.isTrashed() && request.isDeleted()) {
            bubbleBacklinkRepository.deleteAll(allBacklinks);
            // 버블도 실제 삭제할 거면 여기서 bubbleRepository.delete(bubble)도 가능
            return bubble; // 더 이상 업데이트 필요 없음
        }

        for (BubbleBacklink link : backlinksFromBubble) {
            Bubble target = link.getBacklinkBubble();
            if(target.isDeleted()||request.isDeleted()) {
                bubbleBacklinkRepository.delete(link);
            }
            else{
                boolean shouldBeTrashed = request.isTrashed() || target.isTrashed();
                System.out.println(link.getId());
                link.setTrashed(shouldBeTrashed);
            }
        }
        for (BubbleBacklink link : backlinksToBubble) {
            Bubble target = link.getBacklinkBubble();
            if(request.isDeleted()||target.isDeleted()) {
                bubbleBacklinkRepository.delete(link);
            }
            else {
                boolean shouldBeTrashed = request.isTrashed() || link.getBubble().isTrashed() || target.isTrashed();
                System.out.println(link.getId());
                System.out.println(target.getLocalIdx());
                System.out.println(request.isTrashed());
                if(!request.isTrashed() && !link.getBubble().isTrashed()){
                    shouldBeTrashed = false;
                }
                System.out.println(shouldBeTrashed);
                link.setTrashed(shouldBeTrashed);
            }
        }
        bubbleBacklinkRepository.saveAll(backlinksFromBubble);
        bubbleBacklinkRepository.saveAll(backlinksToBubble);


        Set<BubbleLabel> bubbleLabels = labels.stream()
                .map(label -> BubbleLabel.builder().bubble(bubble).label(label).build())
                .collect(Collectors.toSet());
        bubble.update(request.getTitle(), request.getContent(), request.getMainImageUrl(), bubbleLabels);



        // 새로운 backlink만 추가 (중복 방지)
        Set<Bubble> existingBacklinks = bubble.getBacklinks().stream()
                .map(b -> b.getBacklinkBubble()) // 중복 체크용
                .collect(Collectors.toSet());

        for (Bubble backlink : backlinks) {
            if (!existingBacklinks.contains(backlink)) {
                BubbleBacklink newBacklink = BubbleBacklink.builder()
                        .bubble(bubble)
                        .backlinkBubble(backlink)
                        .isTrashed(request.isTrashed())
                        .build();
                bubble.getBacklinks().add(newBacklink);
            }
        }

        bubble.setTrashed(request.isTrashed());
        bubble.setUpdatedAt(request.getUpdatedAt());
        bubble.setDeletedAt(request.getDeletedAt());

        return bubbleRepository.saveAndFlush(bubble);
    }

    private Bubble hardDeleteBubble(BubbleRequestDto.SyncDto request, Member member) {

        Bubble bubble = bubbleRepository.findByMemberAndLocalIdx(member, request.getLocalIdx())
                .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));

        bubbleRepository.delete(bubble);
        return null;
    }

    private Bubble createNewBubble(BubbleRequestDto.SyncDto request, Member member, Set<Bubble> backlinks, Set<Label> labels) {
        Bubble newBubble = Bubble.builder()
                .localIdx(request.getLocalIdx())
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
                    .localIdx(request.getLocalIdx())
                    .title(request.getTitle())
                    .content(request.getContent())
                    .mainImageUrl(request.getMainImageUrl())
                    .labels(mapLabelsToDtoByLocalIdx(member, request.getLabelIdxs()))
                    .backlinkIdxs(request.getBacklinkIds())
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
                .localIdx(bubble.getLocalIdx())
                .title(bubble.getTitle())
                .content(bubble.getContent())
                .mainImageUrl(bubble.getMainImg())
                .labels(mapLabelsToDto(labels))
                .backlinkIdxs(bubble.getBacklinks().stream()
                        .map(BubbleBacklink::getBacklinkBubble)
                        .map(Bubble::getLocalIdx)
                        .collect(Collectors.toSet()))
                .isDeleted(false)
                .isTrashed(bubble.isTrashed())
                .createdAt(bubble.getCreatedAt())
                .updatedAt(bubble.getUpdatedAt())
                .deletedAt(bubble.getDeletedAt())
                .build();
    }

    // 백링크 검증
    private Set<Bubble> validateBacklinks(Set<String> backlinkIdxs, Member member) {
        Set<String> idxs = Optional.ofNullable(backlinkIdxs).orElse(Collections.emptySet());

        if (idxs.isEmpty()) { return Collections.emptySet();}
        Set<Bubble> backlinks = new HashSet<>(bubbleRepository.findAllByMemberAndLocalIdxIn(member, idxs));

        // 조회된 라벨의 localIdx와 요청된 localIdx가 일치하는지 확인
        Set<String> foundIdxs = backlinks.stream().map(Bubble::getLocalIdx).collect(Collectors.toSet());
        if (!foundIdxs.containsAll(idxs)) {
            throw new GeneralException(ErrorStatus.BACKLINK_NOT_FOUND);
        }
        if (!backlinks.stream().allMatch(bubble -> bubble.getMember().equals(member))) {
            throw new GeneralException(ErrorStatus.BACKLINK_FORBIDDEN);
        }

        return new HashSet<>(backlinks);
    }

    private Set<Label> validateLabels(Set<String> labelIdxs, Member member) {
        Set<String> idxs = Optional.ofNullable(labelIdxs).orElse(Collections.emptySet());

        if (idxs.isEmpty()) { return Collections.emptySet();}
        if (idxs.size() > 3) { throw new GeneralException(ErrorStatus.LABELS_TOO_MANY);}

        Set<Label> labels = new HashSet<>(labelRepository.findAllByMemberAndLocalIdxIn(member, idxs));

        // 조회된 라벨의 localIdx와 요청된 localIdx가 일치하는지 확인
        Set<String> foundIdxs = labels.stream().map(Label::getLocalIdx).map(String::valueOf).collect(Collectors.toSet());
        if (!foundIdxs.containsAll(idxs)) { throw new GeneralException(ErrorStatus.LABELS_NOT_FOUND);}

        if (!labels.stream().allMatch(label -> label.getMember().equals(member))) {
            throw new GeneralException(ErrorStatus.LABELS_FORBIDDEN);
        }

        return new HashSet<>(labels);
    }


    // 라벨을 DTO로 변환 (localIdx 기준)
    public List<LabelResponseDTO.LabelSimpleInfoDto> mapLabelsToDtoByLocalIdx(Member member, Set<String> localIdxs) {
        if (localIdxs == null || localIdxs.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Label> labels = labelRepository.findAllByMemberAndLocalIdxIn(member, localIdxs);  // Set으로 받기

        if (labels.size() != localIdxs.size()) {
            throw new GeneralException(ErrorStatus.LABELS_NOT_FOUND);
        }

        return labels.stream()
                .map(l -> LabelResponseDTO.LabelSimpleInfoDto.builder()
                        .localIdx(l.getLocalIdx())
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
