package com.edison.project.domain.artletter.controller;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.artletter.dto.ArtletterDTO;
import com.edison.project.domain.artletter.entity.ArtletterCategory;
import com.edison.project.domain.artletter.service.ArtletterService;
import com.edison.project.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/artletters")
@RequiredArgsConstructor
@Slf4j
public class ArtletterController {

    private final ArtletterService artletterService;

    // 아트레터 등록
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> createArtletter(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Valid @RequestBody ArtletterDTO.CreateRequestDto requestDto) {
        ArtletterDTO.CreateResponseDto response = artletterService.createArtletter(userPrincipal, requestDto);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }


    // 전체 아트레터 조회
    @GetMapping
    public ResponseEntity<ApiResponse> getAllArtletters(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "default") String sortType) {

        if (page < 0 || size <= 0 || size > 100) {
            return ApiResponse.onFailure(ErrorStatus.INVALID_PAGE_REQUEST);
        }

        if (!List.of("default", "likes", "scraps", "latest").contains(sortType)) {
            sortType = "default";
        }

        return artletterService.getAllArtlettersResponse(userPrincipal, page, size, sortType);
    }


    // 아트레터 검색
    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchArtletters(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "default") String sortType) {

        if (page < 0 || size <= 0 || size > 100) {
            return ApiResponse.onFailure(ErrorStatus.INVALID_PAGE_REQUEST);
        }

        if (keyword == null || keyword.trim().isEmpty()) {
            return ApiResponse.onFailure(ErrorStatus.KEYWORD_IS_EMPTY);
        }

        if (!List.of("default", "likes", "scraps", "latest").contains(sortType)) {
            sortType = "default";
        }

        return artletterService.searchArtletters(userPrincipal, keyword.trim(), page, size, sortType);
    }


    @PostMapping("/editor-pick")
    public ResponseEntity<ApiResponse> getEditorArtletters(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestBody ArtletterDTO.EditorRequestDto editorRequestDto) {

        return artletterService.getEditorArtletters(userPrincipal, editorRequestDto);
    }
  

    // 좋아요 기능
    @PostMapping("/{letterId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> likeArtletter(@PathVariable Long letterId, @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        ArtletterDTO.LikeResponseDto response = artletterService.likeToggleArtletter(userPrincipal, letterId);

        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    // 스크랩 기능
    @PostMapping("/{letterId}/scrap")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> scrapArtletter(@PathVariable Long letterId, @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        ArtletterDTO.ScrapResponseDto response = artletterService.scrapToggleArtletter(userPrincipal, letterId);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    // 아트레터 상세 조회
    @GetMapping("/{letterId}")
    public ResponseEntity<ApiResponse> getArtletterInfo(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable("letterId") Long letterId) {
        ArtletterDTO.ListResponseDto response = artletterService.getArtletter(userPrincipal, letterId);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    @GetMapping("/recommend-bar/category")
    public ResponseEntity<ApiResponse> getRecommendCategory() {
        List<String> categories = artletterService.getRecommendCategory();
        return ApiResponse.onSuccess(SuccessStatus._OK, new ArtletterDTO.RecommendCategoryResponse(categories));
    }

    @GetMapping("/recommend-bar/keyword")
    public ResponseEntity<ApiResponse> getRecommendKeywords(
            @RequestParam List<Long> artletterIds) {
        List<ArtletterDTO.recommendKeywordDto> response = artletterService.getRecommendKeyword(artletterIds);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    @GetMapping("/scrap")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getScrapArtlettersByCategory(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return artletterService.getScrapArtlettersByCategory(userPrincipal, pageable);
    }

    @GetMapping("/scrap/{category}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getScrapCategoryArtletters(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable ArtletterCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return artletterService.getScrapCategoryArtletters(userPrincipal, category, pageable);
    }

    // 최근 검색어 조회
    @GetMapping("/search-memory")
    public ResponseEntity<ApiResponse> getMemoryMemory(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        return artletterService.getMemoryKeyword(userPrincipal);
    }

    // 최근 검색어 삭제
    @DeleteMapping("/search-memory")
    public ResponseEntity<ApiResponse> deleteMemoryKeyword(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return artletterService.deleteMemoryKeyword(userPrincipal, keyword);
    }
}
