package com.edison.project.domain.label.controller;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.label.dto.LabelRequestDTO;
import com.edison.project.domain.label.dto.LabelResponseDTO;
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

}
