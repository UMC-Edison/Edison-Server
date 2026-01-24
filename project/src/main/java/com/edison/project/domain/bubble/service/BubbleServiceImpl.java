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
import com.pgvector.PGvector; // Must be imported
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BubbleServiceImpl implements BubbleService {

    private final BubbleRepository bubbleRepository;
    private final LabelRepository labelRepository;
    private final BubbleLabelRepository bubbleLabelRepository;
    private final MemberRepository memberRepository;
    private final BubbleBacklinkRepository bubbleBacklinkRepository;
    private final EmbeddingService embeddingService;
    private final DimensionReductionService dimensionReductionService;

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
                .isDeleted(bubble.isDeleted())
                .createdAt(bubble.getCreatedAt())
                .updatedAt(bubble.getUpdatedAt())
                .deletedAt(bubble.getDeletedAt())
                .build();
    }

    @Override
    public ResponseEntity<ApiResponse> getBubblesByMember(CustomUserPrincipal userPrincipal, Pageable pageable) {
        Page<Bubble> bubblePage = bubbleRepository.findByMember_MemberIdAndIsTrashedFalse(userPrincipal.getMemberId(), pageable);

        List<BubbleResponseDto.SyncResultDto> bubbles = bubblePage.getContent().stream()
                .map(this::convertToBubbleResponseDto)
                .collect(Collectors.toList());

        PageInfo pageInfo = new PageInfo(bubblePage.getNumber(), bubblePage.getSize(), bubblePage.hasNext(),
                bubblePage.getTotalElements(), bubblePage.getTotalPages());

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, bubbles);
    }

    @Override
    public ResponseEntity<ApiResponse> getDeletedBubbles(CustomUserPrincipal userPrincipal, Pageable pageable) {
        Page<Bubble> bubblePage = bubbleRepository.findByMember_MemberIdAndIsTrashedTrue(userPrincipal.getMemberId(), pageable);
        LocalDateTime now = LocalDateTime.now();

        List<BubbleResponseDto.TrashedListResultDto> bubbles = bubblePage.getContent().stream()
                .map(bubble -> convertToTrashedDto(bubble, now))
                .collect(Collectors.toList());

        PageInfo pageInfo = new PageInfo(bubblePage.getNumber(), bubblePage.getSize(), bubblePage.hasNext(),
                bubblePage.getTotalElements(), bubblePage.getTotalPages());

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, bubbles);
    }

    @Override
    public ResponseEntity<ApiResponse> getRecentBubblesByMember(CustomUserPrincipal userPrincipal, Pageable pageable) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Page<Bubble> bubblePage = bubbleRepository.findRecentByMember(userPrincipal.getMemberId(), sevenDaysAgo, pageable);

        List<BubbleResponseDto.SyncResultDto> bubbles = bubblePage.getContent().stream()
                .map(this::convertToBubbleResponseDto)
                .collect(Collectors.toList());

        PageInfo pageInfo = new PageInfo(bubblePage.getNumber(), bubblePage.getSize(), bubblePage.hasNext(),
                bubblePage.getTotalElements(), bubblePage.getTotalPages());

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, bubbles);
    }

    @Override
    public BubbleResponseDto.SyncResultDto getBubble(CustomUserPrincipal userPrincipal, String localIdx) {
        Bubble bubble = bubbleRepository.findByMemberAndLocalIdxWithDetails(userPrincipal.getMemberId(), localIdx)
                .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));
        return convertToBubbleResponseDto(bubble);
    }

    @Override
    @Transactional
    public BubbleResponseDto.SyncResultDto syncBubble(CustomUserPrincipal userPrincipal, BubbleRequestDto.SyncDto request) {
        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Set<Bubble> backlinks = validateBacklinks(request.getBacklinkIds(), member);
        Set<Label> labels = validateLabels(request.getLabelIdxs(), member);

        Bubble bubble = processBubble(request, member, backlinks, labels);
        return buildSyncResultDto(request, bubble, member);
    }

    @Override
    @Transactional
    public BubbleResponseDto.CreateResultDto createBubble(CustomUserPrincipal userPrincipal, BubbleRequestDto.CreateDto requestDto) {
        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        if (bubbleRepository.existsByMemberAndLocalIdx(member, requestDto.getLocalIdx())) {
            throw new GeneralException(ErrorStatus.BUBBLE_ALREADY_EXISTS);
        }

        Set<Bubble> backlinks = validateBacklinks(requestDto.getBacklinkIds(), member);
        Set<Label> labels = validateLabels(requestDto.getLabelIdxs(), member);

        Bubble bubble = Bubble.builder()
                .localIdx(requestDto.getLocalIdx())
                .member(member)
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .mainImg(requestDto.getMainImageUrl())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Set<BubbleBacklink> backlinkEntities = backlinks.stream()
                .map(target -> BubbleBacklink.builder().bubble(bubble).backlinkBubble(target).build())
                .collect(Collectors.toSet());
        bubble.getBacklinks().addAll(backlinkEntities);

        Set<BubbleLabel> labelEntities = labels.stream()
                .map(label -> BubbleLabel.builder().bubble(bubble).label(label).build())
                .collect(Collectors.toSet());
        bubble.getLabels().addAll(labelEntities);

        Bubble savedBubble = bubbleRepository.save(bubble);

        return BubbleResponseDto.CreateResultDto.builder()
                .localIdx(savedBubble.getLocalIdx())
                .title(savedBubble.getTitle())
                .content(savedBubble.getContent())
                .mainImageUrl(savedBubble.getMainImg())
                .labels(mapLabelsToDto(labels))
                .backlinkIdxs(savedBubble.getBacklinks().stream()
                        .map(BubbleBacklink::getBacklinkBubble)
                        .map(Bubble::getLocalIdx)
                        .collect(Collectors.toSet()))
                .createdAt(savedBubble.getCreatedAt())
                .updatedAt(savedBubble.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public BubbleResponseDto.CreateResultDto updateBubble(CustomUserPrincipal userPrincipal, String bubbleLocalIdx, BubbleRequestDto.CreateDto requestDto) {
        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Bubble bubble = bubbleRepository.findByMember_MemberIdAndLocalIdxAndIsTrashedFalse(member.getMemberId(), bubbleLocalIdx)
                .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));

        Set<Bubble> backlinks = validateBacklinks(requestDto.getBacklinkIds(), member);
        Set<Label> labels = validateLabels(requestDto.getLabelIdxs(), member);

        bubble.update(requestDto.getTitle(), requestDto.getContent(), requestDto.getMainImageUrl(), labels, backlinks);
        bubbleRepository.save(bubble);

        return BubbleResponseDto.CreateResultDto.builder()
                .localIdx(bubble.getLocalIdx())
                .title(bubble.getTitle())
                .content(bubble.getContent())
                .mainImageUrl(bubble.getMainImg())
                .labels(mapLabelsToDto(labels))
                .backlinkIdxs(bubble.getBacklinks().stream()
                        .map(BubbleBacklink::getBacklinkBubble)
                        .map(Bubble::getLocalIdx)
                        .collect(Collectors.toSet()))
                .createdAt(bubble.getCreatedAt())
                .updatedAt(bubble.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public BubbleResponseDto.DeleteRestoreResultDto deleteBubble(CustomUserPrincipal userPrincipal, String BubbleLocalIdx){
        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        Bubble bubble = bubbleRepository.findByMember_MemberIdAndLocalIdxAndIsTrashedFalse(member.getMemberId(), BubbleLocalIdx)
                .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));
        bubble.softDelete();
        return BubbleResponseDto.DeleteRestoreResultDto.builder().localIdx(bubble.getLocalIdx()).isTrashed(bubble.isTrashed()).build();
    }

    @Override
    @Transactional
    public BubbleResponseDto.DeleteRestoreResultDto restoreBubble(CustomUserPrincipal userPrincipal, String BubbleLocalIdx){
        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        Bubble bubble = bubbleRepository.findByMember_MemberIdAndLocalIdxAndIsTrashedTrue(member.getMemberId(), BubbleLocalIdx)
                .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));
        bubble.restore();
        return BubbleResponseDto.DeleteRestoreResultDto.builder().localIdx(bubble.getLocalIdx()).isTrashed(bubble.isTrashed()).build();
    }

    @Override
    @Transactional
    public void hardDeleteBubble(CustomUserPrincipal userPrincipal, String bubbleId) {
        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        Bubble bubble = bubbleRepository.findByMember_MemberIdAndLocalIdxAndIsTrashedTrue(member.getMemberId(), bubbleId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));
        bubbleRepository.delete(bubble);
    }

    @Override
    public ResponseEntity<ApiResponse> getAllBubbles(CustomUserPrincipal userPrincipal, Pageable pageable){
        Page<Bubble> bubblePage = bubbleRepository.findByMember_MemberId(userPrincipal.getMemberId(), pageable);
        PageInfo pageInfo = new PageInfo(bubblePage.getNumber(), bubblePage.getSize(), bubblePage.hasNext(),
                bubblePage.getTotalElements(), bubblePage.getTotalPages());
        List<BubbleResponseDto.SyncResultDto> bubbles = bubblePage.getContent().stream()
                .map(this::convertToBubbleResponseDto)
                .collect(Collectors.toList());
        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, bubbles);
    }

    // --- [AI Vectorization Methods] ---

    /**
     * Vectorize a single Bubble
     */
    @Override
    @Transactional
    public BubbleResponseDto.VectorizeResultDto vectorizeBubble(CustomUserPrincipal userPrincipal, String bubbleLocalIdx) {
        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Bubble bubble = bubbleRepository.findByMember_MemberIdAndLocalIdxAndIsTrashedFalse(
                        member.getMemberId(), bubbleLocalIdx)
                .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));

        String textToEmbed = (bubble.getTitle() != null ? bubble.getTitle() : "") + " " +
                (bubble.getContent() != null ? bubble.getContent() : "");

        PGvector embedding = embeddingService.embed(textToEmbed);

        bubble.setEmbedding(embedding.toArray());
        bubble.setUpdatedAt(LocalDateTime.now());

        // embedding.toArray() returns float[], so pass it directly
        float[] vector = embedding.toArray();
        double[][] projection = dimensionReductionService.reduceTo2D(new float[][] { vector });

        bubble.setEmbedding2dX(projection[0][0]);
        bubble.setEmbedding2dY(projection[0][1]);

        bubbleRepository.save(bubble);

        return BubbleResponseDto.VectorizeResultDto.builder()
                .localIdx(bubble.getLocalIdx())
                .title(bubble.getTitle())
                .isVectorized(true)
                .embedding2dX(bubble.getEmbedding2dX())
                .embedding2dY(bubble.getEmbedding2dY())
                .vectorizedAt(LocalDateTime.now())
                .message("Bubble vectorized successfully")
                .build();
    }

    /**
     * Vectorize All Bubbles
     */
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> vectorizeAllBubbles(CustomUserPrincipal userPrincipal) {
        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        List<Bubble> bubbles = bubbleRepository.findByMember_MemberIdAndIsTrashedFalse(member.getMemberId());

        if (bubbles.isEmpty()) {
            return ApiResponse.onSuccess(SuccessStatus._OK, null, "No bubbles found");
        }

        List<BubbleResponseDto.VectorizeResultDto> results = new ArrayList<>();
        List<Bubble> embeddedBubbles = new ArrayList<>();
        List<float[]> tempEmbeddingsList = new ArrayList<>();

        for (Bubble bubble : bubbles) {
            try {
                String textToEmbed = (bubble.getTitle() != null ? bubble.getTitle() : "") + " " +
                        (bubble.getContent() != null ? bubble.getContent() : "");

                PGvector embedding = embeddingService.embed(textToEmbed);

                bubble.setEmbedding(embedding.toArray());
                bubble.setUpdatedAt(LocalDateTime.now());

                embeddedBubbles.add(bubble);
                tempEmbeddingsList.add(embedding.toArray());

            } catch (Exception e) {
                log.error("Failed to vectorize bubble [{}]: {}", bubble.getLocalIdx(), e.getMessage());
                results.add(BubbleResponseDto.VectorizeResultDto.builder()
                        .localIdx(bubble.getLocalIdx())
                        .isVectorized(false)
                        .message("Failed: " + e.getMessage())
                        .build());
            }
        }

        if (!embeddedBubbles.isEmpty()) {
            float[][] successfulEmbeddings = tempEmbeddingsList.toArray(new float[0][]);
            double[][] projection = dimensionReductionService.reduceTo2D(successfulEmbeddings);

            for (int i = 0; i < embeddedBubbles.size(); i++) {
                Bubble bubble = embeddedBubbles.get(i);
                bubble.setEmbedding2dX(projection[i][0]);
                bubble.setEmbedding2dY(projection[i][1]);
                results.add(BubbleResponseDto.VectorizeResultDto.builder()
                        .localIdx(bubble.getLocalIdx())
                        .title(bubble.getTitle())
                        .isVectorized(true)
                        .embedding2dX(bubble.getEmbedding2dX())
                        .embedding2dY(bubble.getEmbedding2dY())
                        .vectorizedAt(LocalDateTime.now())
                        .message("Success")
                        .build());
            }
            bubbleRepository.saveAll(embeddedBubbles);
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("totalRequest", bubbles.size());
        responseData.put("successCount", embeddedBubbles.size());
        responseData.put("results", results);

        return ApiResponse.onSuccess(SuccessStatus._OK, responseData);
    }

    /**
     * Get All Bubble Embeddings (Coordinates)
     * IMPLEMENTATION ADDED HERE TO FIX INTERFACE ERROR
     */
    @Override
    public ResponseEntity<ApiResponse> getAllBubbleEmbeddings(CustomUserPrincipal userPrincipal, Pageable pageable) {
        // Fetch active bubbles
        Page<Bubble> bubblePage = bubbleRepository.findByMember_MemberIdAndIsTrashedFalse(userPrincipal.getMemberId(), pageable);

        // Map to simple DTO containing coordinates
        List<BubbleResponseDto.VectorizeResultDto> dtos = bubblePage.getContent().stream()
                .filter(b -> b.getEmbedding2dX() != null && b.getEmbedding2dY() != null) // Only return vectorized ones
                .map(b -> BubbleResponseDto.VectorizeResultDto.builder()
                        .localIdx(b.getLocalIdx())
                        .title(b.getTitle())
                        .isVectorized(true)
                        .embedding2dX(b.getEmbedding2dX())
                        .embedding2dY(b.getEmbedding2dY())
                        .build())
                .collect(Collectors.toList());

        // You might want to adjust page info to reflect the filtered list,
        // but typically pagination applies to the query result.
        PageInfo pageInfo = new PageInfo(bubblePage.getNumber(), bubblePage.getSize(), bubblePage.hasNext(),
                bubblePage.getTotalElements(), bubblePage.getTotalPages());

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, dtos);
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

        List<BubbleBacklink> backlinksFromBubble = bubbleBacklinkRepository.findByBubble_BubbleId(bubble.getBubbleId());
        List<BubbleBacklink> backlinksToBubble = bubbleBacklinkRepository.findByBacklinkBubble_BubbleId(bubble.getBubbleId());

        Set<BubbleBacklink> allBacklinks = new HashSet<>();
        allBacklinks.addAll(backlinksFromBubble);
        allBacklinks.addAll(backlinksToBubble);

        if (request.isTrashed() && request.isDeleted()) {
            bubbleBacklinkRepository.deleteAll(allBacklinks);
            return bubble;
        }

        // Logic to update backlinks trash status...
        for (BubbleBacklink link : backlinksFromBubble) {
            Bubble target = link.getBacklinkBubble();
            if(target.isDeleted()||request.isDeleted()) {
                bubbleBacklinkRepository.delete(link);
            } else {
                boolean shouldBeTrashed = request.isTrashed() || target.isTrashed();
                link.setTrashed(shouldBeTrashed);
            }
        }
        for (BubbleBacklink link : backlinksToBubble) {
            Bubble target = link.getBacklinkBubble();
            if(request.isDeleted()||target.isDeleted()) {
                bubbleBacklinkRepository.delete(link);
            } else {
                boolean shouldBeTrashed = request.isTrashed() || link.getBubble().isTrashed() || target.isTrashed();
                if(!request.isTrashed() && !link.getBubble().isTrashed()){
                    shouldBeTrashed = false;
                }
                link.setTrashed(shouldBeTrashed);
            }
        }
        bubbleBacklinkRepository.saveAll(backlinksFromBubble);
        bubbleBacklinkRepository.saveAll(backlinksToBubble);

        Set<BubbleLabel> bubbleLabels = labels.stream()
                .map(label -> BubbleLabel.builder().bubble(bubble).label(label).build())
                .collect(Collectors.toSet());
        bubble.update(request.getTitle(), request.getContent(), request.getMainImageUrl(), bubbleLabels);

        Set<Bubble> existingBacklinks = bubble.getBacklinks().stream()
                .map(BubbleBacklink::getBacklinkBubble)
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
        bubbleLabelRepository.saveAll(bubbleLabels);
        savedBubble.getLabels().addAll(bubbleLabels);
        bubbleRepository.saveAndFlush(savedBubble);

        Set<BubbleBacklink> newbacklinks = backlinks.stream()
                .map(backlink -> BubbleBacklink.builder()
                        .bubble(savedBubble)
                        .backlinkBubble(backlink)
                        .build())
                .collect(Collectors.toSet());
        savedBubble.getBacklinks().addAll(newbacklinks);

        return savedBubble;
    }

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

    private Set<Bubble> validateBacklinks(Set<String> backlinkIdxs, Member member) {
        Set<String> idxs = Optional.ofNullable(backlinkIdxs).orElse(Collections.emptySet());
        if (idxs.isEmpty()) { return Collections.emptySet();}
        Set<Bubble> backlinks = new HashSet<>(bubbleRepository.findAllByMemberAndLocalIdxIn(member, idxs));
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
        Set<String> foundIdxs = labels.stream().map(Label::getLocalIdx).map(String::valueOf).collect(Collectors.toSet());
        if (!foundIdxs.containsAll(idxs)) { throw new GeneralException(ErrorStatus.LABELS_NOT_FOUND);}
        if (!labels.stream().allMatch(label -> label.getMember().equals(member))) {
            throw new GeneralException(ErrorStatus.LABELS_FORBIDDEN);
        }
        return new HashSet<>(labels);
    }

    public List<LabelResponseDTO.LabelSimpleInfoDto> mapLabelsToDtoByLocalIdx(Member member, Set<String> localIdxs) {
        if (localIdxs == null || localIdxs.isEmpty()) { return Collections.emptyList(); }
        Set<Label> labels = labelRepository.findAllByMemberAndLocalIdxIn(member, localIdxs);
        if (labels.size() != localIdxs.size()) { throw new GeneralException(ErrorStatus.LABELS_NOT_FOUND); }
        return labels.stream().map(l -> LabelResponseDTO.LabelSimpleInfoDto.builder().localIdx(l.getLocalIdx()).name(l.getName()).color(l.getColor()).build()).collect(Collectors.toList());
    }

    public List<LabelResponseDTO.LabelSimpleInfoDto> mapLabelsToDto(Set<Label> labels) {
        return labels.stream().map(l -> LabelResponseDTO.LabelSimpleInfoDto.builder().localIdx(l.getLocalIdx()).name(l.getName()).color(l.getColor()).build()).collect(Collectors.toList());
    }

    private BubbleResponseDto.TrashedListResultDto convertToTrashedDto(Bubble bubble, LocalDateTime now) {
        LocalDateTime deletedAt = Optional.ofNullable(bubble.getDeletedAt()).orElse(now);
        long remainDays = 30 - ChronoUnit.DAYS.between(deletedAt, now);
        List<LabelResponseDTO.LabelSimpleInfoDto> labelDtos = bubble.getLabels().stream()
                .map(bl -> LabelResponseDTO.LabelSimpleInfoDto.builder().localIdx(bl.getLabel().getLocalIdx()).name(bl.getLabel().getName()).color(bl.getLabel().getColor()).build())
                .collect(Collectors.toList());
        return BubbleResponseDto.TrashedListResultDto.builder()
                .localIdx(bubble.getLocalIdx())
                .title(bubble.getTitle())
                .content(bubble.getContent())
                .labels(labelDtos)
                .createdAt(bubble.getCreatedAt())
                .updatedAt(bubble.getUpdatedAt())
                .deletedAt(bubble.getDeletedAt())
                .remainDay((int) Math.max(remainDays, 0))
                .build();
    }
}