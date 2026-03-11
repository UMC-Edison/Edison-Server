package com.edison.project.domain.artletter.controller;

import com.edison.project.common.response.Response;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.artletter.dto.ArtletterDTO;
import com.edison.project.domain.artletter.entity.ArtletterCategory;
import com.edison.project.domain.artletter.service.ArtletterService;
import com.edison.project.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(
        name = "Artletter",
        description = "아트레터 관련 API - 아트레터 생성, 조회, 검색, 좋아요, 스크랩, 추천바 등"
)
@RestController
@RequestMapping("/artletters")
@RequiredArgsConstructor
@Slf4j
public class ArtletterController {

    private final ArtletterService artletterService;

    // 아트레터 등록
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "아트레터 등록"
    )
    public ResponseEntity<Response> createArtletter(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Valid @RequestBody ArtletterDTO.CreateRequestDto requestDto) {
        ArtletterDTO.CreateResponseDto response = artletterService.createArtletter(userPrincipal, requestDto);
        return Response.onSuccess(SuccessStatus._OK, response);
    }


    // 전체 아트레터 조회
    @GetMapping
    @Operation(
            summary = "전체 아트레터 조회"
    )
    public ResponseEntity<Response> getAllArtletters(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "default") String sortType) {

        if (page < 0 || size <= 0 || size > 100) {
            return Response.onFailure(ErrorStatus.INVALID_PAGE_REQUEST);
        }

        if (!List.of("default", "likes", "scraps", "latest").contains(sortType)) {
            sortType = "default";
        }

        return artletterService.getAllArtlettersResponse(userPrincipal, page, size, sortType);
    }


    // 아트레터 검색
    @GetMapping("/search")
    @Operation(
            summary = "아트레터 검색",
            description = "입력한 키워드를 기준으로 아트레터를 검색합니다. 태그, 제목, 내용에 대한 full text search를 하고, 검색어와의 관련도를 기준으로 결과를 정렬합니다."
    )
    public ResponseEntity<Response> searchArtletters(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "default") String sortType) {

        if (page < 0 || size <= 0 || size > 100) {
            return Response.onFailure(ErrorStatus.INVALID_PAGE_REQUEST);
        }

        if (keyword == null || keyword.trim().isEmpty()) {
            return Response.onFailure(ErrorStatus.KEYWORD_IS_EMPTY);
        }

        if (!List.of("default", "likes", "scraps", "latest").contains(sortType)) {
            sortType = "default";
        }

        return artletterService.searchArtletters(userPrincipal, keyword.trim(), page, size, sortType);
    }


    @GetMapping("/editor-pick")
    @Operation(
            summary = "에디터 픽 아트레터 조회"
    )
    public ResponseEntity<Response> getEditorArtletters(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        List<ArtletterDTO.ListResponseDto> response = artletterService.getEditorArtletters(userPrincipal);
        return Response.onSuccess(SuccessStatus._OK, response);
    }


    // 좋아요 기능
    @PostMapping("/{letterId}/like")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "아트레터 좋아요 토글"
    )
    public ResponseEntity<Response> likeArtletter(@PathVariable Long letterId, @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        ArtletterDTO.LikeResponseDto response = artletterService.likeToggleArtletter(userPrincipal, letterId);

        return Response.onSuccess(SuccessStatus._OK, response);
    }

    // 스크랩 기능
    @PostMapping("/{letterId}/scrap")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "아트레터 스크랩 토글"
    )
    public ResponseEntity<Response> scrapArtletter(@PathVariable Long letterId, @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        ArtletterDTO.ScrapResponseDto response = artletterService.scrapToggleArtletter(userPrincipal, letterId);
        return Response.onSuccess(SuccessStatus._OK, response);
    }

    // 아트레터 상세 조회
    @GetMapping("/{letterId}")
    @Operation(
            summary = "아트레터 상세내용 조회"
    )
    public ResponseEntity<Response> getArtletterInfo(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable("letterId") Long letterId) {
        ArtletterDTO.ListResponseDto response = artletterService.getArtletter(userPrincipal, letterId);
        return Response.onSuccess(SuccessStatus._OK, response);
    }

    @GetMapping("/recommend-bar/category")
    @Operation(
            summary = "추천바 카테고리 조회",
            description = "아트레터 추천바에 들어갈 3개의 카테고리값을 반환합니다."
    )
    public ResponseEntity<Response> getRecommendCategory() {
        List<String> categories = artletterService.getRecommendCategory();
        return Response.onSuccess(SuccessStatus._OK, new ArtletterDTO.RecommendCategoryResponse(categories));
    }

    @GetMapping("/recommend-bar/keyword")
    @Operation(
            summary = "추천바 키워드 조회",
            description = "아트레터 추천바에 들어갈 3개의 키워드값을 반환합니다."
    )
    public ResponseEntity<Response> getRecommendKeywords(
            @RequestParam List<Long> artletterIds) {
        List<ArtletterDTO.recommendKeywordDto> response = artletterService.getRecommendKeyword(artletterIds);
        return Response.onSuccess(SuccessStatus._OK, response);
    }

    @GetMapping("/more/{currentId}")
    @Operation(
            summary = "다른 아트레터 랜덤 조회",
            description = "사용자가 현재 보고 있는 아트레터를 제외한 다른 아트레터 3개를 랜덤으로 조회합니다."
    )
    public ResponseEntity<Response> getRandomArtletters(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable("currentId") Long currentId) {

        List<ArtletterDTO.CategoryResponseDto> response = artletterService.getOtherArtletters(userPrincipal, currentId);
        return Response.onSuccess(SuccessStatus._OK, response);
    }

    @GetMapping("/scrap")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "스크랩한 아트레터 조회"
    )
    public ResponseEntity<Response> getScrapArtletters(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return artletterService.getScrapArtletters(userPrincipal, pageable);
    }

    @GetMapping("/scrap/{category}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "스크랩한 아트레터 카테고리별 조회"
    )
    public ResponseEntity<Response> getScrapCategoryArtletters(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable ArtletterCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return artletterService.getScrapCategoryArtletters(userPrincipal, category, pageable);
    }

    // 최근 검색어 조회
    @GetMapping("/search-memory")
    @Operation(
            summary = "최근 검색어 조회"
    )
    public ResponseEntity<Response> getMemoryMemory(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        return artletterService.getMemoryKeyword(userPrincipal);
    }

    // 최근 검색어 삭제
    @DeleteMapping("/search-memory")
    @Operation(
            summary = "최근 검색어 삭제"
    )
    public ResponseEntity<Response> deleteMemoryKeyword(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return artletterService.deleteMemoryKeyword(userPrincipal, keyword);
    }

    // 카테고리별 아트레터 조회
    @GetMapping("/category")
    @Operation(
            summary = "아트레터 카테고리별 조회"
    )
    public ResponseEntity<Response> getArtlettersByCategory(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam("category") ArtletterCategory category,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return artletterService.getArtlettersByCategory(userPrincipal, category, pageable);
    }
}
