package com.edison.project.domain.bubble.controller;

import com.edison.project.common.response.Response;
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
    public ResponseEntity<Response> getBubblesByMember(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // 최신순 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        ResponseEntity<Response> response = bubbleService.getBubblesByMember(userPrincipal, pageable);
        return response;
    }


    @GetMapping("/deleted")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> getDeletedBubbles(
        @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "deletedAt"));
        return bubbleService.getDeletedBubbles(userPrincipal, pageable);
    }


    // 버블 상세정보 조회
    @GetMapping("/{localIdx}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> getBubble (
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String localIdx) {
        BubbleResponseDto.SyncResultDto response = bubbleService.getBubble(userPrincipal, localIdx);
        return Response.onSuccess(SuccessStatus._OK, response);
    }


    // 7일 내 버블 목록 조회
    @GetMapping("/recent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> getRecentBubblesByMember(
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
    public ResponseEntity<Response> syncBubble(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestBody @Valid BubbleRequestDto.SyncDto request) {
        BubbleResponseDto.SyncResultDto response = bubbleService.syncBubble(userPrincipal, request);
        return Response.onSuccess(SuccessStatus._OK, response);
    }

    // 버블 생성
    @PostMapping
    public ResponseEntity<Response> createBubble(@AuthenticationPrincipal CustomUserPrincipal userPrincipal,
                                                 @RequestBody @Valid BubbleRequestDto.CreateDto request) {
        BubbleResponseDto.CreateResultDto response = bubbleService.createBubble(userPrincipal, request);
        return Response.onSuccess(SuccessStatus._OK, response);

    }

    //버블 삭제
    @PatchMapping("/{bubbleId}/delete")
    public ResponseEntity<Response> deleteBubble(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bubbleId) {
        BubbleResponseDto.DeleteRestoreResultDto result = bubbleService.deleteBubble(userPrincipal, bubbleId);
        return Response.onSuccess(SuccessStatus._OK, result);
    }

    //버블 수정
    @PatchMapping("/{bubbleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> updateBubble(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bubbleId,
            @RequestBody @Valid BubbleRequestDto.CreateDto request) {
        BubbleResponseDto.CreateResultDto response = bubbleService.updateBubble(userPrincipal, bubbleId, request);
        return Response.onSuccess(SuccessStatus._OK, response);
    }

    //버블 복원
    @PatchMapping("/{bubbleId}/restore")
    public ResponseEntity<Response> restoreBubble(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bubbleId) {
        BubbleResponseDto.DeleteRestoreResultDto result = bubbleService.restoreBubble(userPrincipal, bubbleId);
        return Response.onSuccess(SuccessStatus._OK, result);
    }

    // 버블 hard-delete
    @DeleteMapping("/trashbin/{bubbleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> hardDeleteBubble(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bubbleId) {
        bubbleService.hardDeleteBubble(userPrincipal, bubbleId);
        return Response.onSuccess(SuccessStatus._OK);
    }

    // 전체 버블 조회(소프트딜리트 포함)
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> getAllBubbles(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return bubbleService.getAllBubbles(userPrincipal, pageable);
    }

    /**
     * 단일 버블 벡터화
     * POST /bubbles/{localIdx}/vectorize
     */
    @PostMapping("/{localIdx}/vectorize")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> vectorizeBubble(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String localIdx) {
        BubbleResponseDto.VectorizeResultDto result = bubbleService.vectorizeBubble(userPrincipal, localIdx);
        return Response.onSuccess(SuccessStatus._OK, result);
    }

    /**
     * 모든 버블 벡터화
     * POST /bubbles/vectorize-all
     */
    @PostMapping("/vectorize-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> vectorizeAllBubbles(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        return bubbleService.vectorizeAllBubbles(userPrincipal);
    }

    /**
     * 사용자의 모든 버블 2D 벡터 좌표 조회
     * GET /bubbles/embeddings
     */
    @GetMapping("/embeddings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> getAllBubbleEmbeddings(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return bubbleService.getAllBubbleEmbeddings(userPrincipal, pageable);
    }

}

