package com.edison.project.domain.label.service;

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

    @Override
    public List<LabelResponseDTO.ListResultDto> getLabelInfoList(Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

//        사용자에 null값이 들어갈 수 없는 경우, 주석 해제할 것
//        if (memberId == null) {
//            throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
//        }

        List<Object[]> labelInfoList = labelRepository.findLabelInfoByMemberId(memberId);

        return labelInfoList.stream()
                .map(result -> {
                    Label label = (Label) result[0];
                    Long bubbleCount = (Long) result[1];
                    return LabelResponseDTO.ListResultDto.builder()
                            .labelId(label.getLabelId())
                            .name(label.getName())
                            .color(label.getColor().name())
                            .bubbleCount(bubbleCount != null ? bubbleCount : 0L) // 버블 없는 라벨은 0으로 처리
                            .build();
                })
                .collect(Collectors.toList());

    }
}
