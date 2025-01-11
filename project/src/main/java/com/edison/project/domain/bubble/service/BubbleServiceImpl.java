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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        //요청 데이터 확인을 위한 로그 출력
        log.info("📥 [Bubble Create Request] memberId: {}, title: {}, linkedBubbleId: {}, labels: {}",
                requestDto.getMemberId(), requestDto.getTitle(), requestDto.getLinkedBubble(), requestDto.getLabels());


        // Member 조회
        Member member = memberRepository.findById(requestDto.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // linkedBubble 검증
        Bubble linkedBubble = null;
        if (requestDto.getLinkedBubble() != null) {
            linkedBubble = bubbleRepository.findById(requestDto.getLinkedBubble())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.BUBBLE_NOT_FOUND));
        }

        // 라벨 검증 - null이면 빈 리스트 할당, 3개 제한
        List<Long> labelIds = requestDto.getLabels() != null ? requestDto.getLabels() : new ArrayList<>();
        if (labelIds.size() > 3) {
            throw new GeneralException(ErrorStatus.LABELS_TOO_MANY);
        }

        List<Label> labels = labelRepository.findAllById(labelIds);
        if (labels.size() != labelIds.size()) {
            throw new GeneralException(ErrorStatus.LABELS_NOT_FOUND);
        }

        // 버블 생성
        Bubble bubble = Bubble.builder()
                .member(member)
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .mainImg(requestDto.getMainImageUrl())
                .linkedBubble(linkedBubble)
                .build();

        Bubble savedBubble = bubbleRepository.save(bubble);

        // 라벨 저장
        Set<BubbleLabel> bubbleLabels = new HashSet<>();
        if (!labelIds.isEmpty()) {

            bubbleLabels = labels.stream()
                    .map(label -> BubbleLabel.builder()
                            .bubble(savedBubble)
                            .label(label)
                            .build())
                    .collect(Collectors.toSet());
            bubbleLabelRepository.saveAll(bubbleLabels);
        }

        // ResponseDto 반환
        return BubbleResponseDto.CreateResultDto.builder()
                .bubbleId(savedBubble.getBubbleId())
                .title(savedBubble.getTitle())
                .content(savedBubble.getContent())
                .mainImageUrl(savedBubble.getMainImg())
                .labels(labelIds)
                .likedBubble(linkedBubble != null ? linkedBubble.getBubbleId() : null)
                .createAt(savedBubble.getCreatedAt())
                .updateAt(savedBubble.getUpdatedAt())
                .build();
    }

}
