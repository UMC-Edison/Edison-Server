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

    BubbleResponseDto.SyncResultDto getBubble(CustomUserPrincipal userPrincipal, Long bubbleId);

    ResponseEntity<ApiResponse> getBubblesByMember(CustomUserPrincipal userPrincipal, Pageable pageable);

    ResponseEntity<ApiResponse> getRecentBubblesByMember(CustomUserPrincipal userPrincipal, Pageable pageable);

    BubbleResponseDto.SyncResultDto syncBubble(CustomUserPrincipal userPrincipal, BubbleRequestDto.SyncDto requestDto);
}
