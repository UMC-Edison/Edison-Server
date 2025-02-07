package com.edison.project.domain.keywords.controller;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.keywords.dto.KeywordsResponseDto;
import com.edison.project.domain.keywords.service.KeywordsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/identity/")
@RequiredArgsConstructor
public class KeywordsRestController {

    private final KeywordsService keywordsService;

    @GetMapping("/{category}")
    public ResponseEntity<ApiResponse> getKeywordsByCategory( @PathVariable String category) {
        List<KeywordsResponseDto.IdentityKeywordDto> result = keywordsService.getKeywordsByCategory(category);
        return ApiResponse.onSuccess(SuccessStatus._OK, result);
    }

}
