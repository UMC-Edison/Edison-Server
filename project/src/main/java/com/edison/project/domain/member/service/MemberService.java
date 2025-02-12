package com.edison.project.domain.member.service;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.domain.member.dto.MemberRequestDto;
import com.edison.project.domain.member.dto.MemberResponseDto;
import com.edison.project.global.security.CustomUserPrincipal;
import org.springframework.http.ResponseEntity;

public interface MemberService {
    MemberResponseDto.LoginResultDto generateTokensForOidcUser(String email);
    Long createUserIfNotExist(String email);
    ResponseEntity<ApiResponse> registerMember(CustomUserPrincipal userPrincipal, MemberRequestDto.ProfileDto request);
    ResponseEntity<ApiResponse> updateProfile(CustomUserPrincipal userPrincipal, MemberRequestDto.UpdateProfileDto request);
    ResponseEntity<ApiResponse> logout(CustomUserPrincipal userPrincipal);
    ResponseEntity<ApiResponse> refreshAccessToken(String token);
    MemberResponseDto.IdentityTestSaveResultDto saveIdentityTest(CustomUserPrincipal userPrincipal, MemberRequestDto.IdentityTestSaveDto request);
    MemberResponseDto.IdentityKeywordsResultDto getIdentityKeywords(CustomUserPrincipal userPrincipal);
    ResponseEntity<ApiResponse> cancel(CustomUserPrincipal userPrincipal);
    MemberResponseDto.IdentityTestSaveResultDto updateIdentityTest(CustomUserPrincipal userPrincipal, MemberRequestDto.IdentityTestSaveDto request);
    ResponseEntity<ApiResponse> getMember(CustomUserPrincipal userPrincipal);
    ResponseEntity<ApiResponse> processGoogleLogin(String authorizationCode);
}