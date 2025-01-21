package com.edison.project.domain.member.service;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.domain.member.dto.MemberRequestDto;
import com.edison.project.global.security.CustomUserPrincipal;
import org.springframework.http.ResponseEntity;

public interface MemberService {
    ResponseEntity<ApiResponse> generateTokensForOidcUser(String email);
    Long createUserIfNotExist(String email);
    ResponseEntity<ApiResponse> registerMember(CustomUserPrincipal userPrincipal, MemberRequestDto.ProfileDto request);
    ResponseEntity<ApiResponse> updateProfile(CustomUserPrincipal userPrincipal, MemberRequestDto.UpdateProfileDto request);

}
