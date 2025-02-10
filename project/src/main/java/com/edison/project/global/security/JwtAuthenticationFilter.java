package com.edison.project.global.security;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.domain.member.entity.RefreshToken;
import com.edison.project.domain.member.repository.MemberRepository;
import com.edison.project.domain.member.repository.RefreshTokenRepository;
import com.edison.project.domain.member.service.RedisTokenService;
import com.edison.project.global.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisTokenService redisTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
            }

            String token = authHeader.substring(7);

            if (request.getRequestURI().equals("/members/refresh")) {
                // Refresh 요청 처리
                handleRefreshRequest(request, token);
            } else {
                // 일반 요청 처리
                handleAccessTokenRequest(request, token);
            }

            filterChain.doFilter(request, response);
        } catch (GeneralException e) {

            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writeValue(response.getWriter(), ApiResponse.onFailure(e.getErrorStatus()).getBody());
            }

        }
    }

    private void handleAccessTokenRequest(HttpServletRequest request, String token) {

        if (jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.extractUserId(token);

            if (redisTokenService.isTokenBlacklisted(token)) {
                if(!memberRepository.existsByMemberId(userId)){
                    throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
                }
                throw new GeneralException(ErrorStatus.ACCESSTOKEN_EXPIRED);
            }

            String email = jwtUtil.extractEmail(token);

            // Refresh Token 검증
            RefreshToken refreshToken = refreshTokenRepository.findByEmail(email)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.LOGIN_REQUIRED));

            if (jwtUtil.isTokenExpired(refreshToken.getRefreshToken())) {
                // Refresh Token이 만료된 경우 Access Token 블랙리스트 처리
                redisTokenService.addToBlacklist(token, jwtUtil.getRemainingTime(token));
                throw new GeneralException(ErrorStatus.REFRESHTOKEN_EXPIRED);
            }
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    new CustomUserPrincipal(userId, email),
                    token,
                    List.of(new SimpleGrantedAuthority("USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            request.setAttribute("token", token);
        }
    }

    private void handleRefreshRequest(HttpServletRequest request, String token) {
        String refreshToken = request.getHeader("Refresh-Token");

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED); // Refresh Token이 없을 때 예외 처리
        }

        String email = jwtUtil.extractEmail(token);

        if(!memberRepository.existsByEmail(email)){
            throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
        }

        // Refresh Token 검증
        RefreshToken storedRefreshToken = refreshTokenRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LOGIN_REQUIRED));

        if(!jwtUtil.isTokenExpired(token)){
            throw new GeneralException(ErrorStatus.ACCESS_TOKEN_VALID);
        }

        if (jwtUtil.isTokenExpired(storedRefreshToken.getRefreshToken())) {
            throw new GeneralException(ErrorStatus.REFRESHTOKEN_EXPIRED);
        }

        if (!storedRefreshToken.getRefreshToken().equals(refreshToken)) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN); // Refresh Token이 저장된 것과 다를 때 예외 처리
        }

        request.setAttribute("refreshToken", refreshToken);

    }
}