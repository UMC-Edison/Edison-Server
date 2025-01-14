package com.edison.project.domain.label.service;

import com.edison.project.domain.label.dto.LabelResponseDTO;
import com.edison.project.domain.label.entity.Label;
import com.edison.project.domain.bubble.repository.BubbleLabelRepository;
import com.edison.project.domain.label.repository.LabelRepository;
import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabelQueryServiceImpl implements LabelQueryService {
    private final LabelRepository labelRepository;
    private final BubbleLabelRepository bubbleLabelRepository;

    @Override
    public List<LabelResponseDTO.ListResultDto> getLabelListByMemberId(Long memberId) {

//        사용자에 null값이 들어갈 수 없는 경우, 주석 해제할 것
//        if (memberId == null) {
//            throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
//        }

        // 1. (특정 멤버의) 모든 라벨을 조회
        List<Label> labels = labelRepository.findByMember_MemberId(memberId);

        // 2. 라벨별 버블 개수를 조회
        List<Object[]> labelWithBubbleCounts = bubbleLabelRepository.findBubbleCountsByMemberId(memberId);

        // 3. 라벨(ID)과 버블 개수를 맵핑
        Map<Long, Long> bubbleCountMap = labelWithBubbleCounts.stream()
                .collect(Collectors.toMap(
                        result -> ((Label) result[0]).getLabelId(),
                        result -> (Long) result[1]
                ));

        // 4. 모든 라벨에 대한 정보 출력
        return labels.stream()
                .map(label -> LabelResponseDTO.ListResultDto.builder()
                        .labelId(label.getLabelId())
                        .name(label.getName())
                        .color(label.getColor().name())
                        // 버블 없는 라벨에 대해 bubbleCount가 null이 아니라 0으로 뜨도록 설정
                        .bubbleCount(bubbleCountMap.getOrDefault(label.getLabelId(), 0L))
                        .build())
                .collect(Collectors.toList());

    }
}
