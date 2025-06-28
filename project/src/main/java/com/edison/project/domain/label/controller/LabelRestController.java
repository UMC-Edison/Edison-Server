package com.edison.project.domain.label.controller;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.label.dto.LabelRequestDTO;
import com.edison.project.domain.label.dto.LabelResponseDTO;
import com.edison.project.domain.label.service.LabelCommandService;
import com.edison.project.domain.label.service.LabelQueryService;
import com.edison.project.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
        List<LabelResponseDTO.ListResultDto> labels = labelQueryService.getLabelInfoList(userPrincipal);
        return ApiResponse.onSuccess(SuccessStatus._OK, labels);
    }

    @GetMapping("/{localIdx}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getLabelDetail(
            @PathVariable String localIdx,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        LabelResponseDTO.DetailResultDto details = labelQueryService.getLabelDetailInfoList(userPrincipal, localIdx);
        return ApiResponse.onSuccess(SuccessStatus._OK, details);
    }

    @PostMapping("/sync")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> syncLabel(
            @RequestBody @Valid LabelRequestDTO.LabelSyncRequestDTO request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        LabelResponseDTO.LabelSyncResponseDTO response = labelQueryService.syncLabel(userPrincipal, request);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createLabel(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestBody LabelRequestDTO.CreateDto request) {
        LabelResponseDTO.LabelSimpleInfoDto response = labelCommandService.createLabel(userPrincipal, request);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    @PatchMapping("/{labelId}")
    public ResponseEntity<ApiResponse> updateLabel(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable String labelId,
            @RequestBody @Valid LabelRequestDTO.CreateDto request) {
        LabelResponseDTO.LabelSimpleInfoDto response = labelCommandService.updateLabel(userPrincipal, labelId, request);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    @DeleteMapping("/{labelId}")
    public ResponseEntity<ApiResponse> deleteLabel(@AuthenticationPrincipal CustomUserPrincipal userPrincipal, @PathVariable @NotNull String labelId) {
        labelCommandService.deleteLabel(userPrincipal, labelId);
        return ApiResponse.onSuccess(SuccessStatus._OK);

    }
}
