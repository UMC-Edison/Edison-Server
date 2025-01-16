package com.edison.project.domain.bubble.service;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.status.ErrorStatus;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public BubbleResponseDto.CreateResultDto createBubble(BubbleRequestDto.CreateDto requestDto) {
        // Member 조회
        Member member = memberRepository.findById(requestDto.getMemberId())
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
        return BubbleResponseDto.CreateResultDto.builder()
                .bubbleId(savedBubble.getBubbleId())
                .title(savedBubble.getTitle())
                .content(savedBubble.getContent())
                .mainImageUrl(savedBubble.getMainImg())
                .labels(labelIds)
                .linkedBubbleId(Optional.ofNullable(linkedBubble).map(Bubble::getBubbleId).orElse(null))
                .createdAt(savedBubble.getCreatedAt())
                .updatedAt(savedBubble.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public BubbleResponseDto.DeleteResultDto deleteBubble(BubbleRequestDto.DeleteDto requestDto) {

        // Bubble 조회
        Bubble bubble = bubbleRepository.findByBubbleIdAndIsDeletedFalse(requestDto.getBubbleId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));

        // 삭제 권한 확인
        if (!bubble.getMember().getMemberId().equals(requestDto.getMemberId())) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
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
    public BubbleResponseDto.RestoreResultDto restoreBubble(BubbleRequestDto.RestoreDto requestDto) {

        // Bubble 조회
        Bubble bubble = bubbleRepository.findByBubbleIdAndIsDeletedTrue(requestDto.getBubbleId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));

        // 복원 권한 확인
        if(!bubble.getMember().getMemberId().equals(requestDto.getMemberId())) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        bubble.setDeleted(false);
        bubbleRepository.save(bubble);

        return BubbleResponseDto.RestoreResultDto.builder()
                .bubbleId(bubble.getBubbleId())
                .isRestored(!bubble.isDeleted())
                .build();
    }

    public List<String> getCombinedTexts() {
        List<Bubble> bubbles = bubbleRepository.findAll();
        return bubbles.stream()
                .map(bubble -> bubble.getTitle() + " " + bubble.getContent() + " " + getLabels(bubble))
                .collect(Collectors.toList());
    }

    private String getLabels(Bubble bubble) {
        return bubble.getLabels().stream() // Bubble에서 BubbleLabel 리스트 가져오기
                .map(bubbleLabel -> bubbleLabel.getLabel().getName()) // BubbleLabel을 통해 Label의 이름 가져오기
                .collect(Collectors.joining(" ")); // 라벨 이름을 공백으로 구분하여 반환
    }

    public double[][] get2DCoordinates(double[][] data) throws Exception {
        // Fetch combined texts
        List<String> combinedTexts = getCombinedTexts();

        // Calculate TF-IDF
        TfidfVectorizer vectorizer = new TfidfVectorizer();
        Map<Integer, Map<String, Double>> tfIdfMap = vectorizer.calculateTfIdf(combinedTexts);

        // Convert to matrix
        double[][] tfIdfMatrix = tfIdfMap.values().stream()
                .map(map -> map.values().stream().mapToDouble(Double::doubleValue).toArray())
                .toArray(double[][]::new);

        // Perform PCA
        PcaReducer pcaReducer = new PcaReducer();
        return pcaReducer.reduceTo2D(tfIdfMatrix);
    }

}
