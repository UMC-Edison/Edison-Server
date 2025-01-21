package com.edison.project.domain.member.controller;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.domain.member.dto.MemberRequestDto;
import com.edison.project.domain.member.service.MemberService;
import com.edison.project.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

}
