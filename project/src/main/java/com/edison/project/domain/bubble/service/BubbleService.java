package com.edison.project.domain.bubble.service;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.domain.bubble.dto.BubbleRequestDto;
import com.edison.project.domain.bubble.dto.BubbleResponseDto;
import com.edison.project.global.security.CustomUserPrincipal;
import org.springframework.http.ResponseEntity;

import org.springframework.data.domain.Pageable;

public interface BubbleService {
    BubbleResponseDto.ListResultDto createBubble(CustomUserPrincipal userPrincipal, BubbleRequestDto.ListDto requestDto);
    BubbleResponseDto.DeleteResultDto deleteBubble(CustomUserPrincipal userPrincipal, Long bubbleId);
    BubbleResponseDto.RestoreResultDto restoreBubble(CustomUserPrincipal userPrincipal, Long bubbleId);

    ResponseEntity<ApiResponse> getBubblesByMember(Long memberId, Pageable pageable);
    ResponseEntity<ApiResponse> searchBubbles(CustomUserPrincipal userPrincipal, String keyword, boolean recent, Pageable pageable);
}
