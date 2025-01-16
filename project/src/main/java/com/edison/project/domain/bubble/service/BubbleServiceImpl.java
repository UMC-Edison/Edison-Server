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
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
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
        Member member = memberRepository.findById(requestDto.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Bubble linkedBubble = null;
        if (requestDto.getLinkedBubbleId() != null) {
            linkedBubble = bubbleRepository.findById(requestDto.getLinkedBubbleId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));
        }

        Set<Long> labelIds = Optional.ofNullable(requestDto.getLabelIds()).orElse(Collections.emptySet());
        if (labelIds.size() > 3) throw new GeneralException(ErrorStatus.LABELS_TOO_MANY);

        Set<Label> labels = new HashSet<>(labelRepository.findAllById(labelIds));
        if (labels.size() != labelIds.size()) throw new GeneralException(ErrorStatus.LABELS_NOT_FOUND);

        Bubble savedBubble = bubbleRepository.save(Bubble.builder()
                .member(member)
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .mainImg(requestDto.getMainImageUrl())
                .linkedBubble(linkedBubble)
                .labels(new HashSet<>())
                .build());

        Set<BubbleLabel> bubbleLabels = labels.stream()
                .map(label -> BubbleLabel.builder().bubble(savedBubble).label(label).build())
                .collect(Collectors.toSet());

        bubbleLabelRepository.saveAll(bubbleLabels);
        savedBubble.getLabels().addAll(bubbleLabels);

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
        Bubble bubble = bubbleRepository.findByBubbleIdAndIsDeletedFalse(requestDto.getBubbleId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));

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

    @Override
    @Transactional
    public BubbleResponseDto.RestoreResultDto restoreBubble(BubbleRequestDto.RestoreDto requestDto) {
        Bubble bubble = bubbleRepository.findByBubbleIdAndIsDeletedTrue(requestDto.getBubbleId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));

        if (!bubble.getMember().getMemberId().equals(requestDto.getMemberId())) {
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
        return bubble.getLabels().stream()
                .map(bubbleLabel -> bubbleLabel.getLabel().getName())
                .collect(Collectors.joining(" "));
    }

    public double[][] calculateTfIdf(List<String> combinedTexts) {
        Map<String, Integer> termDocCount = new HashMap<>();
        Map<Integer, Map<String, Double>> tfMap = new HashMap<>();

        for (int docId = 0; docId < combinedTexts.size(); docId++) {
            String[] terms = combinedTexts.get(docId).split(" ");
            Map<String, Double> termFrequency = new HashMap<>();
            for (String term : terms) {
                termFrequency.put(term, termFrequency.getOrDefault(term, 0.0) + 1);
            }
            tfMap.put(docId, termFrequency);

            for (String term : termFrequency.keySet()) {
                termDocCount.put(term, termDocCount.getOrDefault(term, 0) + 1);
            }
        }

        double[][] tfIdfMatrix = new double[combinedTexts.size()][];

        for (int docId = 0; docId < combinedTexts.size(); docId++) {
            Map<String, Double> termFrequency = tfMap.get(docId);
            double[] tfIdfValues = new double[termDocCount.size()];
            int index = 0;
            for (String term : termDocCount.keySet()) {
                double tf = termFrequency.getOrDefault(term, 0.0);
                double idf = Math.log((double) combinedTexts.size() / (1 + termDocCount.get(term)));
                tfIdfValues[index++] = tf * idf;
            }
            tfIdfMatrix[docId] = tfIdfValues;
        }
        return tfIdfMatrix;
    }

    public double[][] performPca(double[][] tfIdfMatrix) {
        RealMatrix matrix = MatrixUtils.createRealMatrix(tfIdfMatrix);
        SingularValueDecomposition svd = new SingularValueDecomposition(matrix);
        RealMatrix u = svd.getU();
        return u.getSubMatrix(0, u.getRowDimension() - 1, 0, 1).getData();
    }
}
