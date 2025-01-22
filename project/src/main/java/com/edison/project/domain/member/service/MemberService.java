package com.edison.project.domain.member.service;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.domain.member.dto.MemberResponseDto;
import com.edison.project.global.security.CustomUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;

public interface MemberService {
    MemberResponseDto.LoginResultDto generateTokensForOidcUser(String email);
    Long createUserIfNotExist(String email);
    ResponseEntity<ApiResponse> registerMember(CustomUserPrincipal userPrincipal, MemberResponseDto.ProfileResultDto request);
    ResponseEntity<ApiResponse> logout(CustomUserPrincipal userPrincipal);
    ResponseEntity<ApiResponse> refreshAccessToken(String token);

}
