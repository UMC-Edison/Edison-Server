package com.edison.project.domain.member.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.member.dto.MemberResponseDto;
import com.edison.project.domain.member.entity.RefreshToken;
import com.edison.project.domain.member.repository.MemberRepository;
import com.edison.project.domain.member.repository.RefreshTokenRepository;
import com.edison.project.global.security.CustomUserPrincipal;
import com.edison.project.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.edison.project.domain.member.entity.Member;
import org.springframework.web.bind.annotation.RequestHeader;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.edison.project.common.status.SuccessStatus._OK;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService{

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final RedisTokenService redisTokenService;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> generateTokensForOidcUser(String email) {

        if (!memberRepository.existsByEmail(email)){
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
        else{
            // 이미 존재하는 사용자의 경우
            Long memberId = memberRepository.findByEmail(email).get().getMemberId();
            String accessToken = jwtUtil.generateAccessToken(memberId, email);
            String refreshToken = jwtUtil.generateRefreshToken(memberId, email);

            refreshTokenRepository.deleteByEmail(email);
            RefreshToken tokenEntity = RefreshToken.create(email, refreshToken);
            refreshTokenRepository.save(tokenEntity);

            MemberResponseDto.LoginResultDto dto = MemberResponseDto.LoginResultDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();

            return ApiResponse.onSuccess(_OK, dto);
        }

    }

    @Override
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

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> registerMember(CustomUserPrincipal userPrincipal, MemberResponseDto.ProfileResultDto request) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        if (request.getNickname()==null || request.getNickname() == "") {
            throw new GeneralException(ErrorStatus.NICKNAME_NOT_EXIST);
        }

        member = member.registerProfile(request.getNickname());
        memberRepository.save(member);

        MemberResponseDto.ProfileResultDto response = MemberResponseDto.ProfileResultDto.builder()
                .nickname(member.getNickname())
                .build();

        return ApiResponse.onSuccess(SuccessStatus._OK, response);

    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> logout(CustomUserPrincipal userPrincipal, String accessToken) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        String token = accessToken.substring(7); // "Bearer " 제거
        long ttl = jwtUtil.getRemainingTime(token);

        // 블랙리스트에 추가
        redisTokenService.addToBlacklist(token, ttl);

        refreshTokenRepository.deleteByEmail(userPrincipal.getEmail());

        return ApiResponse.onSuccess(_OK);
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> refreshAccessToken(CustomUserPrincipal userPrincipal) {

        String email = userPrincipal.getEmail();

        RefreshToken refreshToken = refreshTokenRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LOGIN_REQUIRED));

        if (!jwtUtil.validateToken(refreshToken.getRefreshToken())){
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }

        if (jwtUtil.isTokenExpired(refreshToken.getRefreshToken())) {
            throw new GeneralException(ErrorStatus.TOKEN_EXPIRED);
        }

        String newAccessToken = jwtUtil.generateAccessToken(userPrincipal.getMemberId(), email);

        MemberResponseDto.RefreshResultDto response = MemberResponseDto.RefreshResultDto.builder()
                .accessToken(newAccessToken)
                .build();

        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }


}
