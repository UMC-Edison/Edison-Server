package com.edison.project.domain.label.service;

import com.edison.project.domain.bubble.dto.BubbleResponseDto;
import com.edison.project.domain.bubble.entity.Bubble;
import com.edison.project.domain.label.dto.LabelResponseDTO;
import com.edison.project.domain.label.entity.Label;
import com.edison.project.domain.bubble.repository.BubbleLabelRepository;
import com.edison.project.domain.label.repository.LabelRepository;
import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.domain.member.entity.Member;
import com.edison.project.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabelQueryServiceImpl implements LabelQueryService {
    private final LabelRepository labelRepository;
    private final MemberRepository memberRepository;
    private final BubbleLabelRepository bubbleLabelRepository;

    @Override
    public List<LabelResponseDTO.ListResultDto> getLabelInfoList(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
        }

        List<Object[]> labelInfoList = labelRepository.findLabelInfoByMemberId(memberId);

        return labelInfoList.stream()
                .map(result -> {
                    Label label = (Label) result[0];
                    Long bubbleCount = (Long) result[1];
                    return LabelResponseDTO.ListResultDto.builder()
                            .labelId(label.getLabelId())
                            .name(label.getName())
                            .color(label.getColor())
                            .bubbleCount(bubbleCount != null ? bubbleCount : 0L) // 버블 없는 라벨은 0으로 처리
                            .build();
                })
                .collect(Collectors.toList());

    }

    @Override
    public LabelResponseDTO.DetailResultDto getLabelDetailInfoList(Long memberId, Long labelId) {
        // **중복**
        if (!memberRepository.existsById(memberId)) {
            throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
        }

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LABELS_NOT_FOUND));

        // 요청한 라벨이 memberId에 속해 있는지(해당 사용자가 만든 라벨이 맞는지) 검증
        if (!label.getMember().getMemberId().equals(memberId)) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        List<Bubble> bubbles = bubbleLabelRepository.findBubblesByLabelId(labelId);

        // BubbleDetailDto 변환
        //** map 내부의 함수 -> 버블 상세내용조회 api 구현 후 함수로 뽑아 중복 제거 가능
        List<BubbleResponseDto.CreateResultDto> bubbleDetails = bubbles.stream()
                .map(bubble -> BubbleResponseDto.CreateResultDto.builder()
                        .bubbleId(bubble.getBubbleId())
                        .title(bubble.getTitle())
                        .content(bubble.getContent())
                        .mainImageUrl(bubble.getMainImg())
                        .labels(bubble.getLabels().stream()
                                .map(bl -> bl.getLabel().getLabelId())
                                .collect(Collectors.toSet()))
                        .linkedBubbleId(bubble.getLinkedBubble() != null ? bubble.getLinkedBubble().getBubbleId() : null)
                        .createdAt(bubble.getCreatedAt())
                        .updatedAt(bubble.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        // DetailResultDto 반환
        return LabelResponseDTO.DetailResultDto.builder()
                .labelId(label.getLabelId())
                .name(label.getName())
                .color(label.getColor())
                .bubbleCount((long) bubbleDetails.size())
                .bubbles(bubbleDetails)
                .build();
    }


}
