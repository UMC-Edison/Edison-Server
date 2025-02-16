package com.edison.project.domain.member.controller;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.member.dto.MemberRequestDto;
import com.edison.project.domain.member.dto.MemberResponseDto;
import com.edison.project.domain.member.service.MemberService;
import com.edison.project.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberRestController {

    private final MemberService memberService;

    // 회원정보(닉네임) 설정
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> registerMember(@AuthenticationPrincipal CustomUserPrincipal userPrincipal, @RequestBody MemberRequestDto.CreateProfileDto request) {
        return memberService.registerMember(userPrincipal, request);
    }

    // 회원정보 변경
    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse> updateProfile(@AuthenticationPrincipal CustomUserPrincipal userPrincipal, @RequestBody MemberRequestDto.UpdateProfileDto request) {
        return memberService.updateProfile(userPrincipal, request);
    }

    // 회원정보 조회
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getMember(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        return memberService.getMember(userPrincipal);
    }



    @PostMapping("/identity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> saveIdentityTest(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestBody @Valid MemberRequestDto.IdentityTestSaveDto request) {
        MemberResponseDto.IdentityTestSaveResultDto result = memberService.saveIdentityTest(userPrincipal, request);
        return ApiResponse.onSuccess(SuccessStatus._OK, result);
    }

    @GetMapping("/identity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getIdentityKeywords(@AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        MemberResponseDto.IdentityKeywordsResultDto result = memberService.getIdentityKeywords(userPrincipal);
        return ApiResponse.onSuccess(SuccessStatus._OK, result);
    }

    @PatchMapping("/identity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> updateIdentityTest(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestBody @Valid MemberRequestDto.IdentityTestSaveDto request) {
        MemberResponseDto.IdentityTestSaveResultDto result = memberService.updateIdentityTest(userPrincipal, request);
        return ApiResponse.onSuccess(SuccessStatus._OK, result);
    }



    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> logout(@AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        return memberService.logout(userPrincipal);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refreshAccessToken(@RequestHeader("Refresh-Token") String refreshToken) {
        return memberService.refreshAccessToken(refreshToken);
    }

    @DeleteMapping("/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> cancel(@AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        return memberService.cancel(userPrincipal);
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse> googleCallback(@RequestBody MemberRequestDto.GoogleLoginDto request) {
        return memberService.processGoogleLogin(request.getIdToken());
    }

}