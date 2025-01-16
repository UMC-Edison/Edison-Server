package com.edison.project.domain.bubble.controller;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.bubble.dto.BubbleRequestDto;
import com.edison.project.domain.bubble.dto.BubbleResponseDto;
import com.edison.project.domain.bubble.dto.PcaRequestDto;
import com.edison.project.domain.bubble.service.BubbleService;
import com.edison.project.domain.bubble.service.PcaReducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/bubbles")
@RequiredArgsConstructor
public class BubbleRestController {
    private final BubbleService bubbleService;

    // 버블 생성
    @PostMapping
    public ResponseEntity<ApiResponse> createBubble(@RequestBody @Valid BubbleRequestDto.CreateDto request) {
        BubbleResponseDto.CreateResultDto response = bubbleService.createBubble(request);
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
}
