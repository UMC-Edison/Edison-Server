package com.edison.project.domain.label.service;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.domain.label.dto.LabelRequestDTO;
import com.edison.project.domain.label.dto.LabelResponseDTO;
import com.edison.project.domain.label.entity.Label;
import com.edison.project.domain.label.repository.LabelRepository;
import com.edison.project.domain.member.entity.Member;
import com.edison.project.domain.member.repository.MemberRepository;
import com.edison.project.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class LabelCommandServiceImpl implements LabelCommandService {

    private final LabelRepository labelRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public LabelResponseDTO.LabelSimpleInfoDto  createLabel(CustomUserPrincipal userPrincipal, LabelRequestDTO.CreateDto request) {
        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Label label = Label.builder()
                .localIdx(request.getLocalIdx())
                .name(request.getName())
                .color(request.getColor())
                .member(member)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Label savedLabel = labelRepository.save(label);

        return LabelResponseDTO.LabelSimpleInfoDto.builder()
                .localIdx(savedLabel.getLocalIdx())
                .name(savedLabel.getName())
                .color(savedLabel.getColor())
                .build();
    }

    @Override
    @Transactional
    public LabelResponseDTO.LabelSimpleInfoDto updateLabel(CustomUserPrincipal userPrincipal, String localIdx, LabelRequestDTO.CreateDto request) {
        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Optional<Label> label = Optional.ofNullable(labelRepository.findLabelByMemberAndLocalIdx(member, localIdx)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LABELS_NOT_FOUND)));

        // 라벨 이름 길이 검증
        if (request.getName().length() > 20) {
            throw new GeneralException(ErrorStatus.LABEL_NAME_TOO_LONG);
        }

        Label targetLabel = label.get(); // Optional에서 실제 객체 꺼냄
        targetLabel.update(request.getName(), request.getColor());

        return LabelResponseDTO.LabelSimpleInfoDto.builder()
                .localIdx(targetLabel.getLocalIdx())
                .name(targetLabel.getName())
                .color(targetLabel.getColor())
                .build();
    }

    @Override
    @Transactional
    public void deleteLabel(CustomUserPrincipal userPrincipal, String localIdx) {

        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Label label = labelRepository.findLabelByMemberAndLocalIdx(member, localIdx)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LABELS_NOT_FOUND));

        labelRepository.delete(label);

    }
}
