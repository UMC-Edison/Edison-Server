package com.edison.project.domain.space.service;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.domain.space.dto.SpaceMapResponseDto;
import com.edison.project.global.security.CustomUserPrincipal;
import org.apache.coyote.Response;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.awt.print.Pageable;
import java.util.List;

public interface SpaceService {

    List<SpaceMapResponseDto.MapResponseDto> mapBubbles(CustomUserPrincipal userPrincipal);
    List<SpaceMapResponseDto.KeywordResponseDto> mapKeywordBubbles(CustomUserPrincipal userPrincipal, String keyword);
    String generateAndSave(String type);
    //ResponseEntity<ApiResponse> processSpaces(CustomUserPrincipal userPrincipal, List<String> localIdxs, String userIdentityKeywords);
    //ResponseEntity<ApiResponse> processSpaces(CustomUserPrincipal userPrincipal, Pageable pageable, String userIdentityKeywords);

}
