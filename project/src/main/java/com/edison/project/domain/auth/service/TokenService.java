package com.edison.project.domain.auth.service;

import com.edison.project.domain.auth.dto.TokenResponseDto;
import com.edison.project.domain.member.entity.Member;
import com.edison.project.domain.member.repository.MemberRepository;
import com.edison.project.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    public TokenResponseDto generateTokens(String email) {
        // ✅ 이메일을 기반으로 Member 정보 조회
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found with email: " + email));

        Long memberId = member.getMemberId();  // ✅ memberId 가져오기

        // ✅ memberId와 email을 함께 전달
        String accessToken = jwtUtil.generateAccessToken(memberId, email);
        String refreshToken = jwtUtil.generateRefreshToken(memberId, email);

        return new TokenResponseDto(accessToken, refreshToken);
    }
}
