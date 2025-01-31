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

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> registerMember(@AuthenticationPrincipal CustomUserPrincipal userPrincipal, @RequestBody MemberRequestDto.ProfileDto request) {
        return memberService.registerMember(userPrincipal, request);
    }

    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse> updateProfile(@AuthenticationPrincipal CustomUserPrincipal userPrincipal, @RequestBody MemberRequestDto.UpdateProfileDto request) {
        return memberService.updateProfile(userPrincipal, request);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> logout(@AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        return memberService.logout(userPrincipal);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refreshAccessToken(@RequestAttribute("expiredAccessToken") String token) {
        return memberService.refreshAccessToken(token);
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
}
