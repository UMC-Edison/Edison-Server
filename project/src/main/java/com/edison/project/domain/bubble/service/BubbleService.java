package com.edison.project.domain.bubble.service;

import com.edison.project.domain.bubble.dto.BubbleRequestDto;
import com.edison.project.domain.bubble.dto.BubbleResponseDto;

public interface BubbleService {
    BubbleResponseDto.CreateResultDto createBubble(BubbleRequestDto.CreateDto requestDto);
}
