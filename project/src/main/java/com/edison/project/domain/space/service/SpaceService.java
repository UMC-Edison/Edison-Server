package com.edison.project.domain.space.service;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.domain.space.dto.SpaceMapResponseDto;
import com.edison.project.global.security.CustomUserPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface SpaceService {
    List<SpaceMapResponseDto.MapResponseDto> mapBubbles(CustomUserPrincipal userPrincipal);
    //ResponseEntity<ApiResponse> processSpaces(CustomUserPrincipal userPrincipal, List<String> localIdxs, String userIdentityKeywords);
    //ResponseEntity<ApiResponse> processSpaces(CustomUserPrincipal userPrincipal, Pageable pageable, String userIdentityKeywords);

}
