package com.edison.project.domain.label.service;

import com.edison.project.domain.label.dto.LabelRequestDTO;
import com.edison.project.domain.label.dto.LabelResponseDTO;
import com.edison.project.domain.label.entity.Label;
import com.edison.project.domain.label.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class LabelCommandServiceImpl implements LabelCommandService {

    private final LabelRepository labelRepository;

    @Override
    @Transactional
    public LabelResponseDTO.CreateResultDto createLabel(LabelRequestDTO.CreateDto request) {
        Label label = Label.builder()
                .name(request.getName())
                .color(Label.LabelColor.valueOf(request.getColor())) // 라벨 String(Dto) -> enum(Entity) 명시적 변환
                .build();

        Label savedLabel = labelRepository.save(label);

        return LabelResponseDTO.CreateResultDto.builder()
                .id(savedLabel.getLabelId())
                .name(savedLabel.getName())
                .color(savedLabel.getColor().name())
                .build();
    }
}
