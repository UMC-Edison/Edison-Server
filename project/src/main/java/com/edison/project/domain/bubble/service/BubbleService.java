package com.edison.project.domain.bubble.service;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.domain.bubble.dto.BubbleRequestDto;
import com.edison.project.domain.bubble.dto.BubbleResponseDto;
import com.edison.project.global.security.CustomUserPrincipal;
import com.edison.project.domain.bubble.entity.Bubble;

import org.springframework.http.ResponseEntity;

import org.springframework.data.domain.Pageable;

public interface BubbleService {
    //BubbleResponseDto.ListResultDto createBubble(CustomUserPrincipal userPrincipal, BubbleRequestDto.ListDto requestDto);

    //BubbleResponseDto.DeleteResultDto deleteBubble(CustomUserPrincipal userPrincipal, Long bubbleId);

    //BubbleResponseDto.RestoreResultDto restoreBubble(CustomUserPrincipal userPrincipal, Long bubbleId);

    //BubbleResponseDto.ListResultDto updateBubble(CustomUserPrincipal userPrincipal, Long bubbleId, BubbleRequestDto.ListDto requestDto);

    ResponseEntity<ApiResponse> getDeletedBubbles(CustomUserPrincipal userPrincipal, Pageable pageable);

    BubbleResponseDto.ListResultDto getBubble(CustomUserPrincipal userPrincipal, Long bubbleId);

    ResponseEntity<ApiResponse> getBubblesByMember(CustomUserPrincipal userPrincipal, Pageable pageable);

    ResponseEntity<ApiResponse> getRecentBubblesByMember(CustomUserPrincipal userPrincipal, Pageable pageable);

    ResponseEntity<ApiResponse> searchBubbles(CustomUserPrincipal userPrincipal, String keyword, boolean recent, Pageable pageable);

    ResponseEntity<ApiResponse> hardDelteBubble(CustomUserPrincipal userPrincipal, Long bubbleId);

    void deleteExpiredBubble();

    BubbleResponseDto.SyncResultDto syncBubble(CustomUserPrincipal userPrincipal, BubbleRequestDto.SyncDto requestDto);
}
