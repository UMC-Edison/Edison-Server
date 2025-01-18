package com.edison.project.domain.bubble.service;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.domain.bubble.dto.BubbleRequestDto;
import com.edison.project.domain.bubble.dto.BubbleResponseDto;
import com.edison.project.global.security.CustomUserPrincipal;
import com.edison.project.domain.bubble.entity.Bubble;
import org.springframework.http.ResponseEntity;

import org.springframework.data.domain.Pageable;
import java.util.List;

public interface BubbleService {
    BubbleResponseDto.ListResultDto createBubble(CustomUserPrincipal userPrincipal, BubbleRequestDto.ListDto requestDto);
    BubbleResponseDto.DeleteResultDto deleteBubble(CustomUserPrincipal userPrincipal, Long bubbleId);
    BubbleResponseDto.RestoreResultDto restoreBubble(CustomUserPrincipal userPrincipal, Long bubbleId);

    ResponseEntity<ApiResponse> getBubblesByMember(Long memberId, Pageable pageable);
    ResponseEntity<ApiResponse> getDeletedBubbles(CustomUserPrincipal userPrincipal, Pageable pageable);
}
