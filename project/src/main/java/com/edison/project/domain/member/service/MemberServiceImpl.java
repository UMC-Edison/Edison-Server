package com.edison.project.domain.member.service;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.member.dto.MemberResponseDto;
import com.edison.project.domain.member.entity.RefreshToken;
import com.edison.project.domain.member.repository.MemberRepository;
import com.edison.project.domain.member.repository.RefreshTokenRepository;
import com.edison.project.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.edison.project.domain.member.entity.Member;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

import static com.edison.project.common.status.SuccessStatus._OK;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService{

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public ResponseEntity<ApiResponse> generateTokensForOidcUser(String email) {
        Long memberId = createUserIfNotExist(email);
        String accessToken = jwtUtil.generateAccessToken(memberId, email);
        String refreshToken = jwtUtil.generateRefreshToken(memberId, email);

        RefreshToken tokenEntity = RefreshToken.create(email, refreshToken);
        refreshTokenRepository.save(tokenEntity);

        MemberResponseDto.LoginResultDto dto = MemberResponseDto.LoginResultDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        return ApiResponse.onSuccess(_OK, dto);
    }

    public Long createUserIfNotExist(String email) {
        return memberRepository.findByEmail(email)
                .map(Member::getMemberId)
                .orElseGet(() -> {
                    Member member = Member.builder()
                            .email(email)
                            .build();
                    memberRepository.save(member);
                    return member.getMemberId();
                });
    }
}
