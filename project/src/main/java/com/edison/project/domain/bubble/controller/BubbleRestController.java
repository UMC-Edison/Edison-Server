package com.edison.project.domain.bubble.controller;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.bubble.dto.BubbleRequestDto;
import com.edison.project.domain.bubble.dto.BubbleResponseDto;
import com.edison.project.domain.bubble.service.BubbleService;
import com.edison.project.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bubbles")
@RequiredArgsConstructor
public class BubbleRestController {
    private final BubbleService bubbleService;

    // 버블 생성
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> createBubble(@AuthenticationPrincipal CustomUserPrincipal userPrincipal, @RequestBody @Valid BubbleRequestDto.CreateDto request) {
        BubbleResponseDto.CreateResultDto response = bubbleService.createBubble(userPrincipal, request);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    //버블 삭제
    @PatchMapping("/{bubbleId}/delete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> deleteBubble(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable Long bubbleId) {
        BubbleResponseDto.DeleteResultDto result = bubbleService.deleteBubble(userPrincipal, bubbleId);
        return ApiResponse.onSuccess(SuccessStatus._OK, result);
    }

    //버블 복원
    @PatchMapping("/{bubbleId}/restore")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> restoreBubble(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable Long bubbleId) {
        BubbleResponseDto.RestoreResultDto result = bubbleService.restoreBubble(userPrincipal, bubbleId);
        return ApiResponse.onSuccess(SuccessStatus._OK, result);
    }
}
