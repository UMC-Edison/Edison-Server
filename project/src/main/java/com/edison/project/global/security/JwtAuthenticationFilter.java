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
            String requestURI = request.getRequestURI();
            String method = request.getMethod();
            String authHeader = request.getHeader("Authorization");

            // 로그인 없이 접근 가능한 경로 리스트
            List<String> openEndpoints = List.of(
                    "/members/google",
                    "/favicon.ico",
                    "/artletters/recommend-bar/category",
                    "/artletters/recommend-bar/keyword",
                    "/artletters/editor-pick"
            );

            if (authHeader == null){
                if (method.equals("GET") && requestURI.startsWith("/artletters")&& !requestURI.contains("scrap")) {
                    filterChain.doFilter(request, response);
                    return;
                }

                if (requestURI.matches("^/artletters/\\d+$")) { // "/artletters/{letterId}" 패턴 허용
                    filterChain.doFilter(request, response);
                    return;
                }

                // 로그인 없이 접근 가능한 경로는 필터를 통과
                if (openEndpoints.contains(requestURI)) {
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
            }

            String token = authHeader.substring(7);

            if (request.getRequestURI().equals("/members/refresh")) {
                handleRefreshRequest(request, token);
            } else {
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
            String email = jwtUtil.extractEmail(token);

            if (!memberRepository.existsByMemberId(userId)) {
                throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
            }

            if (redisTokenService.isTokenBlacklisted(token)) {
                throw new GeneralException(ErrorStatus.ACCESSTOKEN_EXPIRED);
            }

            RefreshToken refreshToken = refreshTokenRepository.findByEmail(email)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.LOGIN_REQUIRED));

            if (jwtUtil.isTokenExpired(refreshToken.getRefreshToken())) {
                redisTokenService.addToBlacklist(token, jwtUtil.getRemainingTime(token));
                throw new GeneralException(ErrorStatus.REFRESHTOKEN_EXPIRED);
            }

            JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                    new CustomUserPrincipal(userId, email),
                    token,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute("token", token);
        }
        else{
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }
    }

    private void handleRefreshRequest(HttpServletRequest request, String token) {
        String refreshToken = request.getHeader("Refresh-Token");

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        String email = jwtUtil.extractEmail(token);

        if (!memberRepository.existsByEmail(email)) {
            throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
        }

        RefreshToken storedRefreshToken = refreshTokenRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LOGIN_REQUIRED));

        if (!jwtUtil.isTokenExpired(token) && !redisTokenService.isTokenBlacklisted(token)) {
            throw new GeneralException(ErrorStatus.ACCESS_TOKEN_VALID);
        }

        if (jwtUtil.isTokenExpired(storedRefreshToken.getRefreshToken())) {
            throw new GeneralException(ErrorStatus.REFRESHTOKEN_EXPIRED);
        }

        if (!storedRefreshToken.getRefreshToken().equals(refreshToken)) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                new CustomUserPrincipal(null, email), // userId가 없을 수도 있음
                token,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
