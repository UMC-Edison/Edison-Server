package com.edison.project.domain.label.controller;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.label.dto.LabelRequestDTO;
import com.edison.project.domain.label.dto.LabelResponseDTO;
import com.edison.project.domain.label.service.LabelCommandService;
import com.edison.project.domain.label.service.LabelQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<ApiResponse> createLabel(@RequestBody @Valid LabelRequestDTO.CreateAndUpdateDto request) {
        LabelResponseDTO.CreateResultDto response = labelCommandService.createLabel(request);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    @PatchMapping("/{labelId}")
    public ResponseEntity<ApiResponse> updateLabel(
            @PathVariable Long labelId,
            @RequestBody @Valid LabelRequestDTO.CreateAndUpdateDto request) {
        LabelResponseDTO.CreateResultDto response = labelCommandService.updateLabel(labelId, request);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    @DeleteMapping("/{labelId}")
    public ResponseEntity<ApiResponse> deleteLabel(
            @PathVariable Long labelId,
            @RequestBody @Valid LabelRequestDTO.DeleteDto request) {
        labelCommandService.deleteLabel(labelId, request.getMemberId());
        return ApiResponse.onSuccess(SuccessStatus._OK);

    }

    @GetMapping
    public ResponseEntity<ApiResponse> getLabelList(
            @RequestParam Long memberId) {
        List<LabelResponseDTO.ListResultDto> labels = labelQueryService.getLabelInfoList(memberId);
        return ApiResponse.onSuccess(SuccessStatus._OK, labels);
    }

    @GetMapping("/{labelId}")
    public ResponseEntity<ApiResponse> getLabelDetail(
            @PathVariable Long labelId,
            @RequestParam Long memberId) {
        LabelResponseDTO.DetailResultDto details = labelQueryService.getLabelDetailInfoList(memberId, labelId);
        return ApiResponse.onSuccess(SuccessStatus._OK, details);
    }

}
