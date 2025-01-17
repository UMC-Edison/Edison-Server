package com.edison.project.domain.bubble.service;

import com.edison.project.domain.bubble.dto.BubbleRequestDto;
import com.edison.project.domain.bubble.dto.BubbleResponseDto;
import com.edison.project.global.security.CustomUserPrincipal;

public interface BubbleService {
    BubbleResponseDto.CreateResultDto createBubble(CustomUserPrincipal userPrincipal, BubbleRequestDto.CreateDto requestDto);
    BubbleResponseDto.DeleteResultDto deleteBubble(CustomUserPrincipal userPrincipal, Long bubbleId);
    BubbleResponseDto.RestoreResultDto restoreBubble(CustomUserPrincipal userPrincipal, Long bubbleId);
}
