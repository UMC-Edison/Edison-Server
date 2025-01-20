package com.edison.project.global.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.status.ErrorStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public String generateAccessToken(Long memberId, String email) {
        return JWT.create()
                .withSubject(String.valueOf(memberId))
                .withClaim("email", email)
                .withExpiresAt(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .sign(Algorithm.HMAC256(secretKey));
    }

    public String generateRefreshToken(Long memberId, String email) {
        return JWT.create()
                .withSubject(String.valueOf(memberId))
                .withClaim("email", email)
                .withExpiresAt(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .sign(Algorithm.HMAC256(secretKey));
    }

    public boolean validateToken(String token) {
        try {
            JWT.require(Algorithm.HMAC256(secretKey)).build().verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long extractUserId(String token) {
        return Long.parseLong(JWT.decode(token).getSubject());

    }

    public String extractEmail(String token) {
        return JWT.decode(token).getClaim("email").asString();
    }

    public long getRemainingTime(String token) {
        try {
            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(secretKey))
                    .build()
                    .verify(token);
            return decodedJWT.getExpiresAt().getTime() - System.currentTimeMillis();
        } catch (JWTVerificationException e) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(secretKey))
                    .build()
                    .verify(token);

            // 만료 시간 확인
            Date expiration = decodedJWT.getExpiresAt();
            return expiration.before(new Date()); // 만료되었는지 확인
        } catch (JWTVerificationException e) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }
    }
}
