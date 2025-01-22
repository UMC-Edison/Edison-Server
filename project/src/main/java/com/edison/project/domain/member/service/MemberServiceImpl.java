package com.edison.project.domain.member.service;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.member.dto.MemberRequestDto;
import com.edison.project.domain.member.dto.MemberResponseDto;
import com.edison.project.domain.member.entity.RefreshToken;
import com.edison.project.domain.member.repository.MemberRepository;
import com.edison.project.domain.member.repository.RefreshTokenRepository;
import com.edison.project.global.security.CustomUserPrincipal;
import com.edison.project.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.edison.project.domain.member.entity.Member;

import java.util.Objects;
import java.util.List;

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
    public MemberResponseDto.LoginResultDto generateTokensForOidcUser(String email) {

        if (!memberRepository.existsByEmail(email)){
            Long memberId = createUserIfNotExist(email);
            String accessToken = jwtUtil.generateAccessToken(memberId, email);
            String refreshToken = jwtUtil.generateRefreshToken(memberId, email);

            RefreshToken tokenEntity = RefreshToken.create(email, refreshToken);
            refreshTokenRepository.save(tokenEntity);

            return MemberResponseDto.LoginResultDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }
        else{
            // 이미 존재하는 사용자의 경우
            Long memberId = memberRepository.findByEmail(email).get().getMemberId();
            String accessToken = jwtUtil.generateAccessToken(memberId, email);
            String refreshToken = jwtUtil.generateRefreshToken(memberId, email);

            refreshTokenRepository.deleteByEmail(email);
            RefreshToken tokenEntity = RefreshToken.create(email, refreshToken);
            refreshTokenRepository.save(tokenEntity);

             return MemberResponseDto.LoginResultDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
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

    public ResponseEntity<ApiResponse> registerMember(CustomUserPrincipal userPrincipal,  MemberRequestDto.ProfileDto request) {
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
    public ResponseEntity<ApiResponse> updateProfile(CustomUserPrincipal userPrincipal, MemberRequestDto.UpdateProfileDto request) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        if (request.getNickname()==null || request.getNickname() == "") {
            throw new GeneralException(ErrorStatus.NICKNAME_NOT_EXIST);
        }

        if(Objects.equals(member.getNickname(), request.getNickname()) && request.getImageUrl()==null){
            throw new GeneralException(ErrorStatus.NICKNAME_NOT_CHANGED);
        }

        if(Objects.equals(member.getNickname(), request.getNickname()) && Objects.equals(member.getProfileImg(), request.getImageUrl())){
            throw new GeneralException(ErrorStatus.PROFILE_NOT_CHANGED);
        }

        MemberResponseDto.UpdateProfileResultDto response;

        if(request.getImageUrl()==null){
            member.updateNickname(request.getNickname());
            response = MemberResponseDto.UpdateProfileResultDto.builder()
                    .nickname(member.getNickname())
                    .imageUrl(member.getProfileImg())
                    .build();
        }
        else{
            member.updateProfile(request.getNickname(), request.getImageUrl());

            response = MemberResponseDto.UpdateProfileResultDto.builder()
                    .nickname(member.getNickname())
                    .imageUrl(request.getImageUrl())
                    .build();
        }

        return ApiResponse.onSuccess(SuccessStatus._OK, response);

    }
  
    public ResponseEntity<ApiResponse> logout(CustomUserPrincipal userPrincipal) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        String token = (String) authentication.getCredentials();
        long ttl = jwtUtil.getRemainingTime(token);

        // 블랙리스트에 추가
        redisTokenService.addToBlacklist(token, ttl);

        refreshTokenRepository.deleteByEmail(userPrincipal.getEmail());

        return ApiResponse.onSuccess(_OK);
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> refreshAccessToken(String token) {

        Long memberId = jwtUtil.extractUserId(token);
        String email = jwtUtil.extractEmail(token);

        RefreshToken refreshToken = refreshTokenRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LOGIN_REQUIRED));

        if (jwtUtil.isTokenExpired(refreshToken.getRefreshToken())) {
            throw new GeneralException(ErrorStatus.REFRESHTOKEN_EXPIRED);
        }

        String newAccessToken = jwtUtil.generateAccessToken(memberId, email);

        // 전에 발급받은 access token 블랙리스트에 추가
        redisTokenService.addToBlacklist(token, jwtUtil.getRemainingTime(token));

        MemberResponseDto.RefreshResultDto response = MemberResponseDto.RefreshResultDto.builder()
                .accessToken(newAccessToken)
                .build();

        return ApiResponse.onSuccess(SuccessStatus._OK, response);

    }


}
