package com.edison.project.domain.label.service;

import com.edison.project.domain.label.dto.LabelResponseDTO;
import com.edison.project.domain.label.dto.LabelRequestDTO;
import com.edison.project.global.security.CustomUserPrincipal;

public interface LabelCommandService {
    LabelResponseDTO.CreateResultDto createLabel(CustomUserPrincipal userPrincipal, LabelRequestDTO.CreateAndUpdateDto request);
    LabelResponseDTO.CreateResultDto updateLabel(CustomUserPrincipal userPrincipal, Long labelId, LabelRequestDTO.CreateAndUpdateDto request);
    void deleteLabel(CustomUserPrincipal userPrincipal, Long labelId);
}
