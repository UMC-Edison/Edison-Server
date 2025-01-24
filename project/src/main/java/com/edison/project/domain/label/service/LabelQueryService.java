package com.edison.project.domain.label.service;

import java.util.List;

import com.edison.project.domain.label.dto.LabelRequestDTO;
import com.edison.project.domain.label.dto.LabelResponseDTO;
import com.edison.project.global.security.CustomUserPrincipal;

public interface LabelQueryService {
    List<LabelResponseDTO.ListResultDto> getLabelInfoList(CustomUserPrincipal userPrincipal);
    LabelResponseDTO.DetailResultDto getLabelDetailInfoList(CustomUserPrincipal userPrincipal, Long labelId);
    LabelResponseDTO.LabelSyncResponseDTO syncLabel(CustomUserPrincipal userPrincipal, LabelRequestDTO.LabelSyncRequestDTO request);
}
