package com.edison.project.domain.space.controller;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.member.service.MemberService;
import com.edison.project.domain.space.dto.SpaceMapResponseDto;
import com.edison.project.domain.space.dto.SpaceResponseDto;
import com.edison.project.domain.space.service.SpaceService;
import com.edison.project.global.security.CustomUserPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/spaces")
public class SpaceController {

    private final SpaceService spaceService;
    private final MemberService memberService;

    public SpaceController(SpaceService spaceService, MemberService memberService) {
        this.spaceService = spaceService;
        this.memberService = memberService;
    }

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
     */

    @GetMapping("/convert")
    public ResponseEntity<?> convertAllSpaces(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        String userIdentityKeywords = memberService.getCategorizedIdentityKeywords(userPrincipal);
        ResponseEntity<ApiResponse> response = spaceService.processSpaces(userPrincipal, PageRequest.of(0, Integer.MAX_VALUE), userIdentityKeywords);
        List<SpaceResponseDto> spaces = (List<SpaceResponseDto>) response.getBody().getResult();
        return ApiResponse.onSuccess(SuccessStatus._OK, spaces);
    }
}
