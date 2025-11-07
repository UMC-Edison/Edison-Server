package com.edison.project.domain.member.service;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.domain.member.dto.MemberRequestDto;
import com.edison.project.domain.member.dto.MemberResponseDto;
import com.edison.project.global.security.CustomUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

public interface MemberService {
    ResponseEntity<ApiResponse> createProfile(CustomUserPrincipal userPrincipal, MemberRequestDto.CreateProfileDto request);
    ResponseEntity<ApiResponse> updateProfile(CustomUserPrincipal userPrincipal, MemberRequestDto.UpdateProfileDto request);
    ResponseEntity<ApiResponse> getProfile(CustomUserPrincipal userPrincipal);

//    MemberResponseDto.IdentityTestSaveResultDto saveIdentityTest(Long memberId, MemberRequestDto.IdentityTestSaveDto request);
    MemberResponseDto.IdentityKeywordsResultDto getIdentityKeywords(CustomUserPrincipal userPrincipal);
    MemberResponseDto.IdentityTestSaveResultDto updateIdentityTest(CustomUserPrincipal userPrincipal, MemberRequestDto.IdentityTestSaveDto request);

    MemberResponseDto.TokenDto generateTokens(Long memberId, String email);
    MemberResponseDto.LoginResultDto generateTokensForOidcUser(String email);
    ResponseEntity<ApiResponse> logout(CustomUserPrincipal userPrincipal);
    ResponseEntity<ApiResponse> refreshAccessToken(String token);
    ResponseEntity<ApiResponse> cancel(CustomUserPrincipal userPrincipal);
    MemberResponseDto.SignupResultDto processGoogleSignup(String authorizationCode, MemberRequestDto.IdentityTestSaveDto request);
    MemberResponseDto.LoginResultDto processGoogleLogin(String authorizationCode);

    String getCategorizedIdentityKeywords(CustomUserPrincipal userPrincipal);

}