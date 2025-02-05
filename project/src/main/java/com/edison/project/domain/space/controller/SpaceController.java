package com.edison.project.domain.space.controller;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.space.dto.SpaceResponseDto;
import com.edison.project.domain.space.service.SpaceService;
import com.edison.project.global.security.CustomUserPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @GetMapping("/convert") // 스페이스로 변환
    public ResponseEntity<?> convertSpaces(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal, Pageable pageable) {
        ResponseEntity<ApiResponse> response = spaceService.processSpaces(userPrincipal, pageable);
        List<SpaceResponseDto> spaces = (List<SpaceResponseDto>) response.getBody().getResult();
        return ApiResponse.onSuccess(SuccessStatus._OK, spaces);
    }

    @GetMapping("/cluster") // 클러스터의 중심, 반지름 반환
    public ResponseEntity<?> clusterSpaces(){
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }
}
