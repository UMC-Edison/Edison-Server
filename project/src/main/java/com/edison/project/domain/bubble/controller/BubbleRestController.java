package com.edison.project.domain.bubble.controller;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.bubble.dto.BubbleRequestDto;
import com.edison.project.domain.bubble.dto.BubbleResponseDto;
import com.edison.project.domain.bubble.service.BubbleService;
import com.edison.project.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;


@RestController
@RequestMapping("/bubbles")
@RequiredArgsConstructor
public class BubbleRestController {
    private final BubbleService bubbleService;

    // 버블 전체 목록 조회
    @GetMapping("/space")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getBubblesByMember(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // 최신순 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        ResponseEntity<ApiResponse> response = bubbleService.getBubblesByMember(userPrincipal, pageable);
        return response;
    }


    @GetMapping("/deleted")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getDeletedBubbles(
        @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return bubbleService.getDeletedBubbles(userPrincipal, pageable);
    }


    // 버블 상세정보 조회
    @GetMapping("/{bubbleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getBubble (
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable Long bubbleId) {
        BubbleResponseDto.SyncResultDto response = bubbleService.getBubble(userPrincipal, bubbleId);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }


    // 7일 내 버블 목록 조회
    @GetMapping("/recent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getRecentBubblesByMember(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // 최신순 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return bubbleService.getRecentBubblesByMember(userPrincipal, pageable);

    }

    // 버블 SYNC
    @PostMapping("/sync")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> syncBubble(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestBody @Valid BubbleRequestDto.SyncDto request) {
        BubbleResponseDto.SyncResultDto response = bubbleService.syncBubble(userPrincipal, request);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

}

