package com.edison.project.domain.label.service;

import java.util.List;

import com.edison.project.domain.label.dto.LabelRequestDto;
import com.edison.project.domain.label.dto.LabelResponseDto;
import com.edison.project.global.security.CustomUserPrincipal;

public interface LabelQueryService {
    List<LabelResponseDto.ListResultDto> getLabelInfoList(CustomUserPrincipal userPrincipal);
    LabelResponseDto.DetailResultDto getLabelDetailInfoList(CustomUserPrincipal userPrincipal, Long labelId);
    LabelResponseDto.LabelSyncResponseDTO syncLabel(CustomUserPrincipal userPrincipal, LabelRequestDto.LabelSyncRequestDTO request);
}
