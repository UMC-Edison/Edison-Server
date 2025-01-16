package com.edison.project.domain.member.controller;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.domain.member.dto.MemberResponseDto;
import com.edison.project.domain.member.service.MemberService;
import com.edison.project.global.security.CustomUserPrincipal;
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
    public ResponseEntity<ApiResponse> registerMember(@AuthenticationPrincipal CustomUserPrincipal userPrincipal, @RequestBody MemberResponseDto.ProfileResultDto request) {
        return memberService.registerMember(userPrincipal, request);
    }

}
