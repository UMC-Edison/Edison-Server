package com.edison.project.domain.keywords.controller;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.keywords.service.KeywordsService;
import com.edison.project.domain.member.dto.MemberResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/identity/")
@RequiredArgsConstructor
public class KeywordsRestController {

    private final KeywordsService keywordsService;

    @GetMapping("/{category}")
    public ResponseEntity<ApiResponse> getKeywordsByCategory( @PathVariable String category) {
        MemberResponseDto.IdentityKeywordsResultDto result = keywordsService.getKeywordsByCategory(category);
        return ApiResponse.onSuccess(SuccessStatus._OK, result);
    }

}
