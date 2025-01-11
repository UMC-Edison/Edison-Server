package com.edison.project.domain.label.service;

import com.edison.project.domain.label.dto.LabelResponseDTO;
import com.edison.project.domain.label.dto.LabelRequestDTO;

public interface LabelCommandService {
    LabelResponseDTO.CreateResultDto createLabel(LabelRequestDTO.CreateDto request);
    LabelResponseDTO.CreateResultDto updateLabel(Long labelId, LabelRequestDTO.CreateDto request);
}
