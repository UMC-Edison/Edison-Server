package com.edison.project.domain.keywords.controller;

import com.edison.project.common.response.Response;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.keywords.dto.KeywordsResponseDto;
import com.edison.project.domain.keywords.service.KeywordsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/identity")
@RequiredArgsConstructor
@Tag(name = "Keywords", description = "정체성(Identity) 관련 키워드 조회 API")
public class KeywordsRestController {

    private final KeywordsService keywordsService;

    @Operation(summary = "카테고리별 키워드 조회", description = "주어진 카테고리에 해당하는 정체성 키워드 목록을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = KeywordsResponseDto.IdentityKeywordDto.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("/{category}")
    public ResponseEntity<Response> getKeywordsByCategory(@Parameter(description = "카테고리 이름", required = true) @PathVariable String category) {
        List<KeywordsResponseDto.IdentityKeywordDto> result = keywordsService.getKeywordsByCategory(category);
        return Response.onSuccess(SuccessStatus._OK, result);
    }

}
