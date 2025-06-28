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

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "deletedAt"));
        return bubbleService.getDeletedBubbles(userPrincipal, pageable);
    }


    // 버블 상세정보 조회
    @GetMapping("/{localIdx}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getBubble (
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String localIdx) {
        BubbleResponseDto.SyncResultDto response = bubbleService.getBubble(userPrincipal, localIdx);
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

    // 버블 생성
    @PostMapping
    public ResponseEntity<ApiResponse> createBubble(@AuthenticationPrincipal CustomUserPrincipal userPrincipal,
                                                    @RequestBody @Valid BubbleRequestDto.CreateDto request) {
        BubbleResponseDto.CreateResultDto response = bubbleService.createBubble(userPrincipal, request);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);

    }

    //버블 삭제
    @PatchMapping("/{bubbleId}/delete")
    public ResponseEntity<ApiResponse> deleteBubble(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bubbleId) {
        BubbleResponseDto.DeleteRestoreResultDto result = bubbleService.deleteBubble(userPrincipal, bubbleId);
        return ApiResponse.onSuccess(SuccessStatus._OK, result);
    }

    //버블 수정
    @PatchMapping("/{bubbleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> updateBubble(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bubbleId,
            @RequestBody @Valid BubbleRequestDto.CreateDto request) {
        BubbleResponseDto.CreateResultDto response = bubbleService.updateBubble(userPrincipal, bubbleId, request);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    //버블 복원
    @PatchMapping("/{bubbleId}/restore")
    public ResponseEntity<ApiResponse> restoreBubble(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bubbleId) {
        BubbleResponseDto.DeleteRestoreResultDto result = bubbleService.restoreBubble(userPrincipal, bubbleId);
        return ApiResponse.onSuccess(SuccessStatus._OK, result);
    }

    // 버블 hard-delete
    @DeleteMapping("/trashbin/{bubbleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> hardDeleteBubble(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bubbleId) {
        bubbleService.hardDelteBubble(userPrincipal, bubbleId);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

}

