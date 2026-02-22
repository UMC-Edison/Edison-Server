package com.edison.project.domain.space.controller;

import com.edison.project.common.response.Response;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.member.service.MemberService;
import com.edison.project.domain.space.dto.SpaceMapResponseDto;
import com.edison.project.domain.space.service.SpaceService;
import com.edison.project.global.security.CustomUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/spaces")
@Tag(name = "Space", description = "스페이스 관련 API (지도, 유사도, 데이터셋 생성 등)")
public class SpaceController {

    private final SpaceService spaceService;
    private final MemberService memberService;

    public SpaceController(SpaceService spaceService, MemberService memberService) {
        this.spaceService = spaceService;
        this.memberService = memberService;
    }

    // doc2vec 키워드
    @Operation(summary = "키워드 기반 유사 스페이스 맵 조회", description = "사용자와 입력 키워드를 바탕으로 키워드 유사도 맵(버블)을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = SpaceMapResponseDto.KeywordResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @PostMapping("/similarity")
    public ResponseEntity<Response> getKeywordMap(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "검색에 사용할 키워드", required = true) @RequestParam String keyword
    ) {
        List<SpaceMapResponseDto.KeywordResponseDto> space = spaceService.mapKeywordBubbles(userPrincipal, keyword);
        return Response.onSuccess(SuccessStatus._OK, space);
    }

    // doc2vec 기본
    @Operation(summary = "변환된 스페이스 맵 조회", description = "사용자의 전체 스페이스 데이터를 변환하여 지도(버블) 형태로 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = SpaceMapResponseDto.MapResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("/map")
    public ResponseEntity<Response> getConvertedMap(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        List<SpaceMapResponseDto.MapResponseDto> space = spaceService.mapBubbles(userPrincipal);
        return Response.onSuccess(SuccessStatus._OK, space);
    }

    // 데이터셋 생성 openAPI
    @Operation(summary = "데이터셋 문장 생성 및 저장", description = "지정한 타입으로 문장을 생성해 저장하고 생성된 문장을 반환합니다. 기본값은 'poetic'입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 파라미터", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @PostMapping("/generate")
    public ResponseEntity<Response> generateSentence(@Parameter(description = "생성할 문장 유형 (예: poetic)") @RequestParam(defaultValue = "poetic") String type) {
        String sentence = spaceService.generateAndSave(type);
        return Response.onSuccess(SuccessStatus._OK, sentence);
    }

    /*
    @PostMapping("/convert")
    public ResponseEntity<?> convertSelectedSpaces(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestBody List<String> localIdxs) {
        String userIdentityKeywords = memberService.getCategorizedIdentityKeywords(userPrincipal);
        ResponseEntity<ApiResponse> response = spaceService.processSpaces(userPrincipal, localIdxs, userIdentityKeywords);
        List<SpaceResponseDto> spaces = (List<SpaceResponseDto>) response.getBody().getResult();
        return ApiResponse.onSuccess(SuccessStatus._OK, spaces);
    }

    @GetMapping("/convert")
    public ResponseEntity<?> convertAllSpaces(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        String userIdentityKeywords = memberService.getCategorizedIdentityKeywords(userPrincipal);
        ResponseEntity<ApiResponse> response = spaceService.processSpaces(userPrincipal, PageRequest.of(0, Integer.MAX_VALUE), userIdentityKeywords);
        List<SpaceResponseDto> spaces = (List<SpaceResponseDto>) response.getBody().getResult();
        return ApiResponse.onSuccess(SuccessStatus._OK, spaces);
    }
    */

}
