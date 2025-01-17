package com.edison.project.domain.bubble.controller;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.bubble.dto.BubbleRequestDto;
import com.edison.project.domain.bubble.dto.BubbleResponseDto;
import com.edison.project.domain.bubble.service.BubbleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;


@RestController
@RequestMapping("/bubbles")
@RequiredArgsConstructor
public class BubbleRestController {
    private final BubbleService bubbleService;

    // 버블 생성
    @PostMapping
    public ResponseEntity<ApiResponse> createBubble(@RequestBody @Valid BubbleRequestDto.ListDto request) {
        BubbleResponseDto.ListResultDto response = bubbleService.createBubble(request);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    //버블 삭제
    @PatchMapping("/{bubbleId}/delete")
    public ResponseEntity<ApiResponse> deleteBubble(
            @PathVariable Long bubbleId,
            @RequestBody @Valid BubbleRequestDto.DeleteDto request) {
        request.setBubbleId(bubbleId);
        BubbleResponseDto.DeleteResultDto result = bubbleService.deleteBubble(request);
        return ApiResponse.onSuccess(SuccessStatus._OK, result);
    }

    //버블 복원
    @PatchMapping("/{bubbleId}/restore")
    public ResponseEntity<ApiResponse> restoreBubble(
            @PathVariable Long bubbleId,
            @RequestBody @Valid BubbleRequestDto.RestoreDto request) {
        request.setBubbleId(bubbleId);
        BubbleResponseDto.RestoreResultDto result = bubbleService.restoreBubble(request);
        return ApiResponse.onSuccess(SuccessStatus._OK, result);
    }

    // 버블 전체 목록 조회
    @GetMapping("/space")
    public ResponseEntity<ApiResponse> getBubblesByMember(
            @RequestParam Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // 최신순 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        ResponseEntity<ApiResponse> response = bubbleService.getBubblesByMember(memberId, pageable);
        return response;
    }

    // 버블 상세정보 조회
    @GetMapping("/{bubbleId}")
    public ResponseEntity<ApiResponse> getBubble(@PathVariable Long bubbleId) {
        BubbleResponseDto.ListResultDto response = bubbleService.getBubble(bubbleId);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }
}