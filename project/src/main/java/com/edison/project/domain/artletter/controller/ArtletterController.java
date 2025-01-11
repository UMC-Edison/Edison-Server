package com.edison.project.domain.artletter.controller;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.artletter.dto.ArtletterDTO;
import com.edison.project.domain.artletter.service.ArtletterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/artletters")
@RequiredArgsConstructor
public class ArtletterController {

    private final ArtletterService artletterService;

    // POST: Create Artletter
    @PostMapping
    public ResponseEntity<ApiResponse> createArtletter(@RequestBody @Valid ArtletterDTO.CreateRequestDto request) {
        ArtletterDTO.CreateResponseDto response = artletterService.createArtletter(request);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    // GET: Search Artletters by keyword
    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchArtletters(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // 키워드가 없는 경우
        if (keyword == null || keyword.isBlank()) {
            return ApiResponse.onFailure(ErrorStatus.KEYWORD_IS_NOT_VALID);
        }

        // 검색 결과 처리
        ArtletterDTO.ListResponseDto response = artletterService.searchArtletters(keyword, page, size);

        if (response.getArtletters() == null || response.getArtletters().isEmpty()) {
            // 검색 결과가 없을 때 처리
            return ApiResponse.onFailure(ErrorStatus.RESULT_NOT_FOUND);
        }

        // 검색 결과가 있는 경우 성공 응답
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }
}
