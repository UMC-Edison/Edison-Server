package com.edison.project.global.security;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.domain.member.entity.RefreshToken;
import com.edison.project.domain.member.repository.RefreshTokenRepository;
import com.edison.project.domain.member.service.RedisTokenService;
import com.edison.project.global.util.JwtUtil;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)

            throws IOException, ServletException {

        try{
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtil.validateToken(token)) {

                    if (redisTokenService.isTokenBlacklisted(token)) {
                        throw new GeneralException(ErrorStatus.INVALID_TOKEN);
                    }

                    Long userId = jwtUtil.extractUserId(token);
                    String email = jwtUtil.extractEmail(token);

                    // Refresh Token 검증
                    RefreshToken refreshToken = refreshTokenRepository.findByEmail(email)
                            .orElseThrow(() -> new GeneralException(ErrorStatus.LOGIN_REQUIRED));

                    if (jwtUtil.isTokenExpired(refreshToken.getRefreshToken())) {
                        // Refresh Token이 만료된 경우 Access Token 블랙리스트 처리
                        redisTokenService.addToBlacklist(token, jwtUtil.getRemainingTime(token));
                        throw new GeneralException(ErrorStatus.TOKEN_EXPIRED);
                    }

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            new CustomUserPrincipal(userId, email),
                            null,
                            List.of(new SimpleGrantedAuthority("USER"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

            filterChain.doFilter(request, response);
        } catch (GeneralException e) {
            // Custom Exception 처리
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"isSuccess\":false,\"code\":\"" + e.getErrorStatus().getCode() + "\",\"message\":\"" + e.getErrorStatus().getMessage() + "\"}");
        }
    }
}

