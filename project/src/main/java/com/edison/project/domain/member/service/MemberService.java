package com.edison.project.domain.member.service;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.domain.member.dto.MemberResponseDto;
import org.springframework.http.ResponseEntity;

public interface MemberService {
    ResponseEntity<ApiResponse> generateTokensForOidcUser(String email);
    Long createUserIfNotExist(String email);
    ResponseEntity<ApiResponse> registerMember(Long memberId, MemberResponseDto.ProfileResultDto request);

}
