package com.edison.project.domain.space.controller;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.space.dto.SpaceResponseDto;
import com.edison.project.domain.space.service.SpaceService;
import com.edison.project.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;

    // 사용자 버블 데이터 기반 Space 생성 및 처리
    @PostMapping("/process")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> processSpaces(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        List<SpaceResponseDto> spaces = spaceService.processSpaces(userPrincipal);
        return ApiResponse.onSuccess(SuccessStatus._OK, spaces);
    }
}
