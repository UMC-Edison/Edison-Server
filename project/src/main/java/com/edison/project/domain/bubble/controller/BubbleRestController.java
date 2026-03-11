package com.edison.project.domain.bubble.controller;

import com.edison.project.common.response.Response;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.bubble.dto.BubbleRequestDto;
import com.edison.project.domain.bubble.dto.BubbleResponseDto;
import com.edison.project.domain.bubble.service.BubbleService;
import com.edison.project.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;

@Tag(name = "Bubble", description = "버블 도메인 API")
@RestController
@RequestMapping("/bubbles")
@RequiredArgsConstructor
public class BubbleRestController {
    private final BubbleService bubbleService;

    @Operation(summary = "삭제되지 않은 버블 전체 목록 조회", description = "soft delete된 버블을 제외한 전체 목록을 조회하는 기능입니다.")
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

    @Operation(summary = "soft delete된 버블 전체 목록 조회", description = "soft delete된 버블 전체 목록을 조회하는 기능입니다.")
    @GetMapping("/deleted")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> getDeletedBubbles(
        @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "deletedAt"));
        return bubbleService.getDeletedBubbles(userPrincipal, pageable);
    }


    @Operation(summary = "버블 상세 조회", description = "localIdx로 버블 상세 정보를 조회하는 기능입니다.")
    @GetMapping("/{localIdx}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> getBubble (
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String localIdx) {
        BubbleResponseDto.SyncResultDto response = bubbleService.getBubble(userPrincipal, localIdx);
        return Response.onSuccess(SuccessStatus._OK, response);
    }


    @Operation(summary = "최근 버블 목록 조회", description = "7일 내 작성된 버블 목록을 조회하는 기능입니다.")
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


    @Operation(summary = "버블 sync", description = "로컬 버블을 서버로 보내 로컬과 서버를 sync하는 기능입니다.")
    @PostMapping("/sync")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> syncBubble(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestBody @Valid BubbleRequestDto.SyncDto request) {
        BubbleResponseDto.SyncResultDto response = bubbleService.syncBubble(userPrincipal, request);
        return Response.onSuccess(SuccessStatus._OK, response);
    }


    @Operation(summary = "버블 생성", description = "버블을 생성하는 기능입니다.")
    @PostMapping
    public ResponseEntity<Response> createBubble(@AuthenticationPrincipal CustomUserPrincipal userPrincipal,
                                                 @RequestBody @Valid BubbleRequestDto.CreateDto request) {
        BubbleResponseDto.CreateResultDto response = bubbleService.createBubble(userPrincipal, request);
        return Response.onSuccess(SuccessStatus._OK, response);

    }


    @Operation(summary = "버블 soft delete", description = "bubbleId로 버블을 soft delete하는 기능입니다.")
    @PatchMapping("/{bubbleId}/delete")
    public ResponseEntity<Response> deleteBubble(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bubbleId) {
        BubbleResponseDto.DeleteRestoreResultDto result = bubbleService.deleteBubble(userPrincipal, bubbleId);
        return Response.onSuccess(SuccessStatus._OK, result);
    }


    @Operation(summary = "버블 수정", description = "bubbleId로 버블을 수정하는 기능입니다.")
    @PatchMapping("/{bubbleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> updateBubble(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bubbleId,
            @RequestBody @Valid BubbleRequestDto.CreateDto request) {
        BubbleResponseDto.CreateResultDto response = bubbleService.updateBubble(userPrincipal, bubbleId, request);
        return Response.onSuccess(SuccessStatus._OK, response);
    }


    @Operation(summary = "버블 복원", description = "bubbleId로 soft delete된 버블을 복원하는 기능입니다.")
    @PatchMapping("/{bubbleId}/restore")
    public ResponseEntity<Response> restoreBubble(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bubbleId) {
        BubbleResponseDto.DeleteRestoreResultDto result = bubbleService.restoreBubble(userPrincipal, bubbleId);
        return Response.onSuccess(SuccessStatus._OK, result);
    }


    @Operation(summary = "버블 hard delete", description = "bubbleId로 버블을 hard delete하는 기능입니다.")
    @DeleteMapping("/trashbin/{bubbleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> hardDeleteBubble(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String bubbleId) {
        bubbleService.hardDeleteBubble(userPrincipal, bubbleId);
        return Response.onSuccess(SuccessStatus._OK);
    }


    @Operation(summary = "전체 버블 목록 조회", description = "소프트딜리트 된 버블을 포함한 모든 버블 목록을 조회하는 기능입니다.")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> getAllBubbles(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return bubbleService.getAllBubbles(userPrincipal, pageable);
    }


    @Operation(summary = "단일 버블 벡터화", description = "localIdx로 soft delete되지 않은 버블을 벡터화하는 기능입니다.")
    @PostMapping("/{localIdx}/vectorize")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> vectorizeBubble(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String localIdx) {
        BubbleResponseDto.VectorizeResultDto result = bubbleService.vectorizeBubble(userPrincipal, localIdx);
        return Response.onSuccess(SuccessStatus._OK, result);
    }


    @Operation(summary = "모든 버블 벡터화", description = "soft delete되지 않은 모든 버블을 벡터화하는 기능입니다.")
    @PostMapping("/vectorize-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> vectorizeAllBubbles(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        return bubbleService.vectorizeAllBubbles(userPrincipal);
    }


    @Operation(summary = "모든 버블 벡터 조회", description = "soft delete되지 않은 모든 버블의 벡터를 조회하는 기능입니다.")
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

