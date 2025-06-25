package com.edison.project.domain.space.service;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.global.security.CustomUserPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface SpaceService {

    ResponseEntity<ApiResponse> processSpaces(CustomUserPrincipal userPrincipal, Pageable pageable, String userIdentityKeywords);
}
