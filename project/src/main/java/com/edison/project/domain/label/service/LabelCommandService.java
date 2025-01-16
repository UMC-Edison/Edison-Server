package com.edison.project.domain.label.service;

import com.edison.project.domain.label.dto.LabelResponseDTO;
import com.edison.project.domain.label.dto.LabelRequestDTO;

public interface LabelCommandService {
    LabelResponseDTO.CreateResultDto createLabel(LabelRequestDTO.CreateAndUpdateDto request);
    LabelResponseDTO.CreateResultDto updateLabel(Long labelId, LabelRequestDTO.CreateAndUpdateDto request);
    void deleteLabel(Long labelId, Long memberId);
}
