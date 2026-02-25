package com.edison.project.domain.label.controller;

import com.edison.project.common.response.Response;
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

// Swagger/OpenAPI
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/labels")
@RequiredArgsConstructor
@Validated
@Tag(name = "Label", description = "라벨(태그) 관리 API")
public class LabelRestController {
    private final LabelCommandService labelCommandService;
    private final LabelQueryService labelQueryService;

    @Operation(summary = "라벨 리스트 조회", description = "사용자의 라벨 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = LabelResponseDTO.ListResultDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> getLabelList(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        List<LabelResponseDTO.ListResultDto> labels = labelQueryService.getLabelInfoList(userPrincipal);
        return Response.onSuccess(SuccessStatus._OK, labels);
    }

    @Operation(summary = "라벨 상세 조회", description = "특정 라벨의 상세 정보를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = LabelResponseDTO.DetailResultDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @GetMapping("/{localIdx}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> getLabelDetail(
            @Parameter(description = "라벨 로컬 인덱스") @PathVariable String localIdx,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        LabelResponseDTO.DetailResultDto details = labelQueryService.getLabelDetailInfoList(userPrincipal, localIdx);
        return Response.onSuccess(SuccessStatus._OK, details);
    }

    @Operation(summary = "라벨 동기화", description = "라벨을 동기화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = LabelResponseDTO.LabelSyncResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @PostMapping("/sync")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> syncLabel(
            @RequestBody @Valid LabelRequestDTO.LabelSyncRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        LabelResponseDTO.LabelSyncResponseDTO response = labelQueryService.syncLabel(userPrincipal, request);
        return Response.onSuccess(SuccessStatus._OK, response);
    }

    @Operation(summary = "라벨 생성", description = "새 라벨을 생성합니다.")
    @PostMapping
    public ResponseEntity<Response> createLabel(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestBody LabelRequestDTO.CreateDto request) {
        LabelResponseDTO.LabelSimpleInfoDto response = labelCommandService.createLabel(userPrincipal, request);
        return Response.onSuccess(SuccessStatus._OK, response);
    }

    @Operation(summary = "라벨 수정", description = "기존 라벨을 수정합니다.")
    @PatchMapping("/{labelId}")
    public ResponseEntity<Response> updateLabel(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "라벨 ID") @PathVariable String labelId,
            @RequestBody @Valid LabelRequestDTO.CreateDto request) {
        LabelResponseDTO.LabelSimpleInfoDto response = labelCommandService.updateLabel(userPrincipal, labelId, request);
        return Response.onSuccess(SuccessStatus._OK, response);
    }

    @Operation(summary = "라벨 삭제", description = "라벨을 삭제합니다.")
    @DeleteMapping("/{labelId}")
    public ResponseEntity<Response> deleteLabel(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "라벨 ID") @PathVariable @NotNull String labelId) {
        labelCommandService.deleteLabel(userPrincipal, labelId);
        return Response.onSuccess(SuccessStatus._OK);

    }
}
