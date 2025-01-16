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


}
