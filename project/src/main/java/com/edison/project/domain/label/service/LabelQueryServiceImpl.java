package com.edison.project.domain.label.service;

import com.edison.project.domain.bubble.dto.BubbleResponseDto;
import com.edison.project.domain.bubble.entity.Bubble;
import com.edison.project.domain.label.dto.LabelRequestDTO;
import com.edison.project.domain.label.dto.LabelResponseDTO;
import com.edison.project.domain.label.entity.Label;
import com.edison.project.domain.bubble.repository.BubbleLabelRepository;
import com.edison.project.domain.label.repository.LabelRepository;
import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.domain.member.entity.Member;
import com.edison.project.domain.member.repository.MemberRepository;
import com.edison.project.global.security.CustomUserPrincipal;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabelQueryServiceImpl implements LabelQueryService {
    private final LabelRepository labelRepository;
    private final MemberRepository memberRepository;
    private final BubbleLabelRepository bubbleLabelRepository;

    // 라벨 목록 조회
    @Override
    public List<LabelResponseDTO.ListResultDto> getLabelInfoList(@AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        if (!memberRepository.existsById(userPrincipal.getMemberId())) {
            throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
        }

        List<Object[]> labelInfoList = labelRepository.findLabelInfoByMemberId(userPrincipal.getMemberId());

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

    // 라벨 상세 조회
    @Override
    public LabelResponseDTO.DetailResultDto getLabelDetailInfoList(@AuthenticationPrincipal CustomUserPrincipal userPrincipal, Long labelId) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        // **중복**
        if (!memberRepository.existsById(userPrincipal.getMemberId())) {
            throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
        }

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LABELS_NOT_FOUND));

        // 요청한 라벨이 memberId에 속해 있는지(해당 사용자가 만든 라벨이 맞는지) 검증
        if (!label.getMember().getMemberId().equals(userPrincipal.getMemberId())) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        List<Bubble> bubbles = bubbleLabelRepository.findBubblesByLabelId(labelId);

        List<BubbleResponseDto.ListResultDto> bubbleDetails = bubbles.stream()
                .map(this::convertToBubbleResponseDto)
                .collect(Collectors.toList());

        // BubbleDetailDto 변환
        return LabelResponseDTO.DetailResultDto.builder()
                .labelId(label.getLabelId())
                .name(label.getName())
                .color(label.getColor())
                .bubbleCount((long) bubbleDetails.size())
                .bubbles(bubbleDetails)
                .build();

        }

    // Bubble -> BubbleResponseDto 변환 함수 (중복 제거)
    private BubbleResponseDto.ListResultDto convertToBubbleResponseDto(Bubble bubble) {
        List<LabelResponseDTO.CreateResultDto> labelDtos = bubble.getLabels().stream()
                .map(bl -> LabelResponseDTO.CreateResultDto.builder()
                        .labelId(bl.getLabel().getLabelId())
                        .name(bl.getLabel().getName())
                        .color(bl.getLabel().getColor())
                        .build())
                .collect(Collectors.toList());

        return BubbleResponseDto.ListResultDto.builder()
                .bubbleId(bubble.getBubbleId())
                .title(bubble.getTitle())
                .content(bubble.getContent())
                .mainImageUrl(bubble.getMainImg())
                .labels(labelDtos) // 라벨 정보 리스트
                .linkedBubbleId(bubble.getLinkedBubble() != null ? bubble.getLinkedBubble().getBubbleId() : null)
                .createdAt(bubble.getCreatedAt())
                .updatedAt(bubble.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public LabelResponseDTO.LabelSyncResponseDTO syncLabel(@AuthenticationPrincipal CustomUserPrincipal userPrincipal, LabelRequestDTO.LabelSyncRequestDTO request) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

//        if (!memberRepository.existsById(userPrincipal.getMemberId())) {
//            throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
//        }

        Label label;

        if (Boolean.TRUE.equals(request.getIsDeleted())) {
            // 라벨 삭제(hard delete)
            label = labelRepository.findById(request.getLabelId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.LABELS_NOT_FOUND));

            if (!label.getMember().getMemberId().equals(userPrincipal.getMemberId())) {
                throw new GeneralException(ErrorStatus._FORBIDDEN);
            }

            label.setDeletedAt(LocalDateTime.now());
            labelRepository.save(label);
            labelRepository.deleteById(label.getLabelId());
            return LabelResponseDTO.LabelSyncResponseDTO.builder()
                    .labelId(request.getLabelId())
                    .isDeleted(true)
                    .deletedAt(label.getDeletedAt())
                    .build();
        }

        // 라벨 업데이트 로직
        if (labelRepository.existsById(request.getLabelId())) {
            label = labelRepository.findById(request.getLabelId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.LABELS_NOT_FOUND));

            if (!label.getMember().getMemberId().equals(userPrincipal.getMemberId())) {
                throw new GeneralException(ErrorStatus._FORBIDDEN);
            }

            label.setName(request.getName());
            label.setColor(request.getColor());
            label.setCreatedAt(request.getCreatedAt());
            label.setUpdatedAt(request.getUpdatedAt());

            labelRepository.save(label);
        }

        // 라벨 생성 로직
        else {
            Member member = memberRepository.findById(userPrincipal.getMemberId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

            label = Label.builder()
                    .labelId(request.getLabelId())
                    .name(request.getName())
                    .color(request.getColor())
                    .member(member)
                    .build();

            labelRepository.save(label);
            label.setCreatedAt(request.getCreatedAt());
            label.setUpdatedAt(request.getUpdatedAt());
            labelRepository.save(label);
        }

        return LabelResponseDTO.LabelSyncResponseDTO.builder()
                .labelId(label.getLabelId())
                .name(label.getName())
                .color(label.getColor())
                .isDeleted(false)
                .createdAt(label.getCreatedAt())
                .updatedAt(label.getUpdatedAt())
                .deletedAt(label.getDeletedAt())
                .build();

    }



}
