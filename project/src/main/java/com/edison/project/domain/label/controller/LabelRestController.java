package com.edison.project.domain.label.controller;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.label.dto.LabelRequestDto;
import com.edison.project.domain.label.dto.LabelResponseDto;
import com.edison.project.domain.label.service.LabelCommandService;
import com.edison.project.domain.label.service.LabelQueryService;
import com.edison.project.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/labels")
@RequiredArgsConstructor
@Validated
public class LabelRestController {
    private final LabelCommandService labelCommandService;
    private final LabelQueryService labelQueryService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getLabelList(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        List<LabelResponseDto.ListResultDto> labels = labelQueryService.getLabelInfoList(userPrincipal);
        return ApiResponse.onSuccess(SuccessStatus._OK, labels);
    }

    @GetMapping("/{localIdx}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getLabelDetail(
            @PathVariable Long localIdx,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        LabelResponseDto.DetailResultDto details = labelQueryService.getLabelDetailInfoList(userPrincipal, localIdx);
        return ApiResponse.onSuccess(SuccessStatus._OK, details);
    }

    @PostMapping("/sync")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> syncLabel(
            @RequestBody @Valid LabelRequestDto.LabelSyncRequestDTO request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        LabelResponseDto.LabelSyncResponseDTO response = labelQueryService.syncLabel(userPrincipal, request);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

}
