package com.edison.project.domain.artletter.controller;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.artletter.dto.ArtletterDTO;
import com.edison.project.domain.artletter.dto.TestDTO;
import com.edison.project.domain.artletter.entity.Artletter;
import com.edison.project.domain.artletter.service.ArtletterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/artletters")
@RequiredArgsConstructor
public class ArtletterController {

    private final ArtletterService artletterService;

    // POST: Create Artletter
    @PostMapping
    public ResponseEntity<ApiResponse> createArtletter(@RequestBody Map<String, Object> request) {
        // 필드 값 추출
        Object readTimeObj = request.get("readTime");
        Object titleObj = request.get("title");
        Object writerObj = request.get("writer");
        Object contentObj = request.get("content");
        Object tagObj = request.get("tag");
        Object categoryObj = request.get("category");
        Object thumbnailObj = request.get("thumbnail");

        // readTime 검증
        if (readTimeObj == null || !(readTimeObj instanceof Integer) || (Integer) readTimeObj <= 0) {
            return ApiResponse.onFailure(ErrorStatus.READTIME_VALIDATION, "readTime은 0보다 큰 정수여야 합니다.");
        }

        // title 검증
        if (titleObj == null || !(titleObj instanceof String) || ((String) titleObj).isBlank()) {
            return ApiResponse.onFailure(ErrorStatus.TITLE_VALIDATION, "title은 비어있을 수 없습니다.");
        }
        if (((String) titleObj).length() > 20) {
            return ApiResponse.onFailure(ErrorStatus.TITLE_VALIDATION, "title은 최대 20자까지 허용됩니다.");
        }

        // writer 검증
        if (writerObj == null || !(writerObj instanceof String)) {
            return ApiResponse.onFailure(ErrorStatus.WRITER_VALIDATION, "writer는 null일 수 없으며 문자열이어야 합니다.");
        }
        if (((String) writerObj).isBlank()) {
            return ApiResponse.onFailure(ErrorStatus.WRITER_VALIDATION, "writer는 비어있을 수 없습니다.");
        }
        if (!((String) writerObj).matches("^[a-zA-Z가-힣\\s]+$")) {
            return ApiResponse.onFailure(ErrorStatus.WRITER_VALIDATION, "writer는 문자만 허용됩니다.");
        }

        // content 검증
        if (contentObj == null || !(contentObj instanceof String) || ((String) contentObj).isBlank()) {
            return ApiResponse.onFailure(ErrorStatus.CONTENT_VALIDATION, "content는 비어있을 수 없습니다.");
        }

        // tag 검증
        if (tagObj == null || !(tagObj instanceof String) || ((String) tagObj).isBlank()) {
            return ApiResponse.onFailure(ErrorStatus.TAG_VALIDATION, "tag는 비어있을 수 없습니다.");
        }
        if (((String) tagObj).length() > 50) {
            return ApiResponse.onFailure(ErrorStatus.TAG_VALIDATION, "tag는 최대 50자까지 허용됩니다.");
        }

        // category 검증
        if (categoryObj == null || !(categoryObj instanceof String)) {
            return ApiResponse.onFailure(ErrorStatus.CATEGORY_VALIDATION, "category는 null일 수 없습니다.");
        }
        try {
            Artletter.ArtletterCategory category = Artletter.ArtletterCategory.valueOf((String) categoryObj);
        } catch (IllegalArgumentException e) {
            return ApiResponse.onFailure(ErrorStatus.CATEGORY_VALIDATION, "category 값이 유효하지 않습니다.");
        }

        // DTO 생성
        ArtletterDTO.CreateRequestDto dto = new ArtletterDTO.CreateRequestDto();
        dto.setReadTime((Integer) readTimeObj);
        dto.setTitle((String) titleObj);
        dto.setWriter((String) writerObj);
        dto.setContent((String) contentObj);
        dto.setTag((String) tagObj);
        dto.setCategory(Artletter.ArtletterCategory.valueOf((String) categoryObj));
        dto.setThumbnail((String) thumbnailObj);

        // Service 호출
        ArtletterDTO.CreateResponseDto response = artletterService.createArtletter(dto);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    // GET: 전체 아트레터 조회
    @GetMapping
    public ResponseEntity<ApiResponse> getAllArtletters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        TestDTO response = artletterService.getAllArtletters(page, size);

        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    // GET: 키워드 기반 search 기능
    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchArtletters(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if(keyword == null) {
            // 키워드 비어있을 때 에러핸들링
            return ApiResponse.onFailure(ErrorStatus.KEYWORD_IS_EMPTY);
        }

        if (keyword.trim().isEmpty()) {
            // 검색 결과가 없을 때 처리
            return ApiResponse.onFailure(ErrorStatus.RESULT_NOT_FOUND);
        }


        Pageable pageable = PageRequest.of(page, size);
        Page<Artletter> results = artletterService.searchArtletters(keyword, pageable);

        return ApiResponse.onSuccess(SuccessStatus._OK, results.getContent());
    }
}
