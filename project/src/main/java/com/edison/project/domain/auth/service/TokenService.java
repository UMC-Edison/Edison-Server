package com.edison.project.domain.auth.service;

import com.edison.project.domain.auth.dto.TokenResponseDto;
import com.edison.project.domain.member.entity.Member;
import com.edison.project.domain.member.entity.RefreshToken;
import com.edison.project.domain.member.repository.MemberRepository;
import com.edison.project.domain.member.repository.RefreshTokenRepository;
import com.edison.project.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository; // ✅ Refresh Token 저장을 위해 추가

    @Transactional
    public TokenResponseDto generateTokens(String email) {
        // ✅ 이메일을 기반으로 Member 정보 조회
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found with email: " + email));

        Long memberId = member.getMemberId();  // ✅ memberId 가져오기

        // ✅ Access Token 및 Refresh Token 생성
        String accessToken = jwtUtil.generateAccessToken(memberId, email);
        String refreshToken = jwtUtil.generateRefreshToken(memberId, email);

        // ✅ 기존 Refresh Token 삭제 후 새로 저장
        refreshTokenRepository.deleteByEmail(email);  // 기존 토큰 삭제
        refreshTokenRepository.save(new RefreshToken(email, refreshToken));  // 새 토큰 저장

        return new TokenResponseDto(accessToken, refreshToken);
    }
}
