package com.edison.project.domain.label.service;

import java.util.List;
import com.edison.project.domain.label.dto.LabelResponseDTO;
public interface LabelQueryService {
    List<LabelResponseDTO.ListResultDto> getLabelListByMemberId(Long memberId);
}
