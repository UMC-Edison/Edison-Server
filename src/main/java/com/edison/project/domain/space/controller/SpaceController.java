package com.edison.project.domain.space.controller;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.member.service.MemberService;
import com.edison.project.domain.space.dto.SpaceMapResponseDto;
import com.edison.project.domain.space.dto.WordResultDto;
import com.edison.project.domain.space.service.SpaceService;
import com.edison.project.domain.space.service.StdicService;
import com.edison.project.global.security.CustomUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/spaces")
public class SpaceController {

    private final SpaceService spaceService;
    private final MemberService memberService;
    private final StdicService stdicService;

    public SpaceController(SpaceService spaceService, MemberService memberService, StdicService stdicService) {
        this.spaceService = spaceService;
        this.memberService = memberService;
        this.stdicService = stdicService;
    }

    // doc2vec 키워드
    @PostMapping("/similarity")
    public ResponseEntity<ApiResponse> getKeywordMap(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam String keyword
    ) {
        List<SpaceMapResponseDto.KeywordResponseDto> space = spaceService.mapKeywordBubbles(userPrincipal, keyword);
        return ApiResponse.onSuccess(SuccessStatus._OK, space);
    }

    // doc2vec 기본
    @GetMapping("/map")
    public ResponseEntity<ApiResponse> getConvertedMap(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        List<SpaceMapResponseDto.MapResponseDto> space = spaceService.mapBubbles(userPrincipal);
        return ApiResponse.onSuccess(SuccessStatus._OK, space);
    }

    // 데이터셋 생성 openAPI
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse> generateSentence(@RequestParam(defaultValue = "poetic") String type) {
        String sentence = spaceService.generateAndSave(type);
        return ApiResponse.onSuccess(SuccessStatus._OK, sentence);
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

    @GetMapping(value = "/dictionary", produces = "application/json; charset=UTF-8")
    public ResponseEntity<ApiResponse> getWords() {
        List<String> words = stdicService.getATierWords();

        Map<String, List<WordResultDto>> resultMap = new LinkedHashMap<>();

        for (String w : words) {
            // String cleanedwords = w.replaceAll("\\d+$", "");
            resultMap.put(w, stdicService.searchAndSave(w));
        }

        return ApiResponse.onSuccess(SuccessStatus._OK, resultMap);
    }

}

