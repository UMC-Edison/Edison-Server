package com.edison.project.domain.label.service;

import com.edison.project.domain.label.dto.LabelResponseDTO;
import com.edison.project.domain.label.dto.LabelRequestDTO;
import com.edison.project.global.security.CustomUserPrincipal;

public interface LabelCommandService {
    LabelResponseDTO.LabelSimpleInfoDto  createLabel(CustomUserPrincipal userPrincipal, LabelRequestDTO.CreateDto request);
    LabelResponseDTO.LabelSimpleInfoDto updateLabel(CustomUserPrincipal userPrincipal, String localIdx, LabelRequestDTO.CreateDto request);
    void deleteLabel(CustomUserPrincipal userPrincipal, String labelId);
}
