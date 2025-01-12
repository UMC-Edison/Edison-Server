package com.edison.project.domain.label.service;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.domain.label.dto.LabelRequestDTO;
import com.edison.project.domain.label.dto.LabelResponseDTO;
import com.edison.project.domain.label.entity.Label;
import com.edison.project.domain.label.repository.LabelRepository;
import com.edison.project.domain.member.entity.Member;
import com.edison.project.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class LabelCommandServiceImpl implements LabelCommandService {

    private final LabelRepository labelRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public LabelResponseDTO.CreateResultDto createLabel(LabelRequestDTO.CreateDto request) {
        Member member = memberRepository.findById(request.getUserId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // 라벨 이름 길이 검증
        if (request.getName().length() > 20) {
            throw new GeneralException(ErrorStatus.LABEL_NAME_TOO_LONG);
        }

        // 라벨 색상 Enum 값 검증
        try {
            Label.LabelColor.valueOf(request.getColor());
        } catch (IllegalArgumentException e) {
            throw new GeneralException(ErrorStatus.INVALID_COLOR);
        }

        Label label = Label.builder()
                .name(request.getName())
                .color(Label.LabelColor.valueOf(request.getColor())) // 라벨 String -> enum 명시적 변환
                .member(member)
                .build();

        Label savedLabel = labelRepository.save(label);

        return LabelResponseDTO.CreateResultDto.builder()
                .labelId(savedLabel.getLabelId())
                .name(savedLabel.getName())
                .color(savedLabel.getColor().name())
                .build();
    }
}
