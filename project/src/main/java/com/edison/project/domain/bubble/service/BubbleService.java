package com.edison.project.domain.bubble.service;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.domain.bubble.dto.BubbleRequestDto;
import com.edison.project.domain.bubble.dto.BubbleResponseDto;
import com.edison.project.global.security.CustomUserPrincipal;
import com.edison.project.domain.bubble.entity.Bubble;

import org.springframework.http.ResponseEntity;

import org.springframework.data.domain.Pageable;

public interface BubbleService {

    ResponseEntity<ApiResponse> getDeletedBubbles(CustomUserPrincipal userPrincipal, Pageable pageable);

    BubbleResponseDto.SyncResultDto getBubble(CustomUserPrincipal userPrincipal, String bubbleIdx);

    ResponseEntity<ApiResponse> getBubblesByMember(CustomUserPrincipal userPrincipal, Pageable pageable);

    ResponseEntity<ApiResponse> getRecentBubblesByMember(CustomUserPrincipal userPrincipal, Pageable pageable);

    BubbleResponseDto.SyncResultDto syncBubble(CustomUserPrincipal userPrincipal, BubbleRequestDto.SyncDto requestDto);

    BubbleResponseDto.CreateResultDto createBubble(CustomUserPrincipal userPrincipal, BubbleRequestDto.CreateDto requestDto);
    BubbleResponseDto.CreateResultDto updateBubble(CustomUserPrincipal userPrincipal, String BubbleLocalIdx, BubbleRequestDto.CreateDto requestDto);
    BubbleResponseDto.DeleteRestoreResultDto deleteBubble(CustomUserPrincipal userPrincipal, String BubbleLocalIdx);
    BubbleResponseDto.DeleteRestoreResultDto restoreBubble(CustomUserPrincipal userPrincipal, String BubbleLocalIdx);
    void hardDelteBubble(CustomUserPrincipal userPrincipal, String bubbleId);
}
