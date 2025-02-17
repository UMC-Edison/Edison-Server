package com.edison.project.domain.label.service;

import com.edison.project.domain.bubble.dto.BubbleResponseDto;
import com.edison.project.domain.bubble.entity.Bubble;
import com.edison.project.domain.bubble.entity.BubbleBacklink;
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
import java.util.Optional;
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

        if (!memberRepository.existsById(userPrincipal.getMemberId())) {
            throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
        }

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LABELS_NOT_FOUND));

        checkOwnership(label, userPrincipal);

        List<Bubble> bubbles = bubbleLabelRepository.findBubblesByLabelId(labelId);

        List<BubbleResponseDto.SyncResultDto> bubbleDetails = bubbles.stream()
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
    private BubbleResponseDto.SyncResultDto convertToBubbleResponseDto(Bubble bubble) {
        List<LabelResponseDTO.LabelSimpleInfoDto> labelDtos = bubble.getLabels().stream()
                .map(bl -> LabelResponseDTO.LabelSimpleInfoDto.builder()
                        .labelId(bl.getLabel().getLabelId())
                        .name(bl.getLabel().getName())
                        .color(bl.getLabel().getColor())
                        .build())
                .collect(Collectors.toList());

        return BubbleResponseDto.SyncResultDto.builder()
                .bubbleId(bubble.getBubbleId())
                .title(bubble.getTitle())
                .content(bubble.getContent())
                .mainImageUrl(bubble.getMainImg())
                .labels(labelDtos) // 라벨 정보 리스트
                .backlinkIds(bubble.getBacklinks().stream()
                        .map(BubbleBacklink::getBacklinkBubble)
                        .map(Bubble::getBubbleId)
                        .collect(Collectors.toSet()))
                .isTrashed(bubble.isTrashed())
                .createdAt(bubble.getCreatedAt())
                .updatedAt(bubble.getUpdatedAt())
                .deletedAt(bubble.getDeletedAt())
                .build();
    }

    @Override
    @Transactional
    public LabelResponseDTO.LabelSyncResponseDTO syncLabel(@AuthenticationPrincipal CustomUserPrincipal userPrincipal, LabelRequestDTO.LabelSyncRequestDTO request) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        // 라벨 삭제
        if (Boolean.TRUE.equals(request.getIsDeleted())) {
            return labelDeletion(request, userPrincipal);
        }

        // 라벨 수정
        if (labelRepository.existsById(request.getLabelId())) {
            return labelUpdate(request, userPrincipal);
        }

        //라벨 생성
        return labelCreation(request, userPrincipal);
    }

    private LabelResponseDTO.LabelSyncResponseDTO labelDeletion(LabelRequestDTO.LabelSyncRequestDTO request, CustomUserPrincipal userPrincipal) {
        if (!labelRepository.existsByLabelId(request.getLabelId())) {
            return buildLabelResponse(request.getLabelId(), request.getName(), request.getColor(), request.getIsDeleted(), request.getCreatedAt(), request.getUpdatedAt(), request.getDeletedAt());
        }

        Label label = labelRepository.findById(request.getLabelId()).orElseThrow(() -> new GeneralException(ErrorStatus.LABELS_NOT_FOUND));
        checkOwnership(label, userPrincipal);
        labelRepository.delete(label);
        return buildLabelResponse(request.getLabelId(), request.getName(), request.getColor(), request.getIsDeleted(), request.getCreatedAt(), request.getUpdatedAt(), request.getDeletedAt());
    }

    private LabelResponseDTO.LabelSyncResponseDTO labelUpdate(LabelRequestDTO.LabelSyncRequestDTO request, CustomUserPrincipal userPrincipal) {
        Label label = labelRepository.findById(request.getLabelId()).orElseThrow(() -> new GeneralException(ErrorStatus.LABELS_NOT_FOUND));
        checkOwnership(label, userPrincipal);

        label.setName(request.getName());
        label.setColor(request.getColor());
        label.setCreatedAt(request.getCreatedAt());
        label.setUpdatedAt(request.getUpdatedAt());

        labelRepository.save(label);
        return buildLabelResponse(label.getLocalIdx(), label.getName(), label.getColor(), false, label.getCreatedAt(), label.getUpdatedAt(), label.getDeletedAt());
    }

    // 라벨 생성 처리
    private LabelResponseDTO.LabelSyncResponseDTO labelCreation(LabelRequestDTO.LabelSyncRequestDTO request, CustomUserPrincipal userPrincipal) {
        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Label label = Label.builder()
                .localIdx(request.getLabelId())
                .name(request.getName())
                .color(request.getColor())
                .member(member)
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .deletedAt(request.getDeletedAt())
                .build();

        labelRepository.save(label);

        return buildLabelResponse(label.getLocalIdx(), label.getName(), label.getColor(), false, label.getCreatedAt(), label.getUpdatedAt(), label.getDeletedAt());
    }

    private LabelResponseDTO.LabelSyncResponseDTO buildLabelResponse(Long localIdx, String name, int color, boolean isDeleted, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        return LabelResponseDTO.LabelSyncResponseDTO.builder()
                .localIdx(localIdx)
                .name(name)
                .color(color)
                .isDeleted(isDeleted)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .build();
    }

    private void checkOwnership(Label label, CustomUserPrincipal userPrincipal) {
        if (!label.getMember().getMemberId().equals(userPrincipal.getMemberId())) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }
    }

}
