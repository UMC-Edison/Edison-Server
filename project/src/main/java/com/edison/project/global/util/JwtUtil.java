package com.edison.project.global.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.status.ErrorStatus;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Collections;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")  // ✅ 기존 설정 값 사용
    private String CLIENT_ID;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    // ✅ Access Token 생성 (Long memberId 포함)
    public String generateAccessToken(Long memberId, String email) {
        return JWT.create()
                .withSubject(String.valueOf(memberId))  // memberId를 String으로 변환하여 subject에 저장
                .withClaim("email", email)
                .withExpiresAt(new Date(System.currentTimeMillis() + accessTokenExpiration * 1000))
                .sign(Algorithm.HMAC256(secretKey));
    }

    // ✅ Refresh Token 생성 (Long memberId 포함)
    public String generateRefreshToken(Long memberId, String email) {
        return JWT.create()
                .withSubject(String.valueOf(memberId))
                .withClaim("email", email)
                .withExpiresAt(new Date(System.currentTimeMillis() + refreshTokenExpiration * 1000))
                .sign(Algorithm.HMAC256(secretKey));
    }


    // ✅ 토큰 검증
    public boolean validateToken(String token) {
        try {
            JWT.require(Algorithm.HMAC256(secretKey))
                    .build()
                    .verify(token);
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    // ✅ 토큰 만료 여부 확인
    public boolean isTokenExpired(String token) {
        try {
            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(secretKey))
                    .build()
                    .verify(token);

            return decodedJWT.getExpiresAt().before(new Date());  // 현재 시간과 비교하여 만료 여부 확인
        } catch (TokenExpiredException e) {
            return true;  // 토큰이 이미 만료됨
        } catch (JWTVerificationException e) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }
    }

    // ✅ 토큰 만료까지 남은 시간 (밀리초 단위 반환)
    public long getRemainingTime(String token) {
        try {
            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(secretKey))
                    .build()
                    .verify(token);
            return decodedJWT.getExpiresAt().getTime() - System.currentTimeMillis();
        } catch (TokenExpiredException e) {
            return 0;  // 이미 만료된 경우 0 반환
        } catch (JWTVerificationException e) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }
    }

    // ✅ 토큰에서 userId 추출
    public Long extractUserId(String token) {
        try {
            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(secretKey))
                    .build()
                    .verify(token);
            return Long.parseLong(decodedJWT.getSubject());  // subject에 저장된 userId 추출
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }
    }

    // ✅ 토큰에서 email 추출
    public String extractEmail(String token) {
        try {
            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(secretKey))
                    .build()
                    .verify(token);
            return decodedJWT.getClaim("email").asString();
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }
    }

    public Payload verifyGoogleIdToken(String idTokenString) {
        try {
            // ✅ 1️⃣ 토큰이 비어 있는 경우
            if (idTokenString == null || idTokenString.trim().isEmpty()) {
                throw new GeneralException(ErrorStatus.EMPTY_TOKEN, "ID Token이 제공되지 않았습니다.");
            }

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(CLIENT_ID))  // 내 앱의 Client ID인지 확인
                    .setIssuer("https://accounts.google.com")  // Google이 발급한 토큰인지 확인
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            // ✅ 2️⃣ 토큰이 유효하지 않은 경우
            if (idToken == null) {
                throw new GeneralException(ErrorStatus.INVALID_TOKEN, "유효하지 않은 Google ID Token입니다.");
            }

            Payload payload = idToken.getPayload();

            // ✅ 3️⃣ 발급자(issuer)가 올바르지 않은 경우
            if (!"https://accounts.google.com".equals(payload.getIssuer())) {
                throw new GeneralException(ErrorStatus.INVALID_ISSUER, "발급자가 Google이 아닙니다.");
            }

            // ✅ 4️⃣ 만료된 토큰인지 확인
            if (payload.getExpirationTimeSeconds() != null) {
                long expirationTime = payload.getExpirationTimeSeconds() * 1000;
                if (new Date().getTime() > expirationTime) {
                    throw new GeneralException(ErrorStatus.REFRESHTOKEN_EXPIRED, "Google ID Token이 만료되었습니다.");
                }
            }

            // ✅ 5️⃣ 클라이언트 ID가 일치하지 않는 경우
            if (!CLIENT_ID.equals(payload.getAudience())) {
                throw new GeneralException(ErrorStatus.INVALID_AUDIENCE, "Google ID Token의 Audience가 일치하지 않습니다.");
            }

            System.out.println("✅ Google ID Token 검증 성공!");
            System.out.println("Issuer: " + payload.getIssuer());
            System.out.println("Email: " + payload.getEmail());
            System.out.println("Email Verified: " + payload.getEmailVerified());
            System.out.println("Name: " + payload.get("name"));
            System.out.println("Picture URL: " + payload.get("picture"));
            System.out.println("Expiration Time: " + payload.getExpirationTimeSeconds());

            return payload;

        } catch (GeneralException e) {
            throw e; // 기존 GeneralException을 그대로 던짐
        } catch (Exception e) {
            // ✅ 6️⃣ 토큰 디코딩 시도하여 원인 확인
            try {
                String[] tokenParts = idTokenString.split("\\.");
                if (tokenParts.length != 3) {
                    throw new GeneralException(ErrorStatus.INVALID_TOKEN, "Google ID Token 형식이 올바르지 않습니다.");
                }

                System.out.println("🔍 JWT Header: " + new String(Base64.getDecoder().decode(tokenParts[0])));
                System.out.println("🔍 JWT Payload: " + new String(Base64.getDecoder().decode(tokenParts[1])));

            } catch (Exception decodeException) {
                System.err.println("⚠️ 토큰 디코딩 오류: " + decodeException.getMessage());
            }

            throw new GeneralException(ErrorStatus.INVALID_TOKEN,
                    "Google ID Token 검증 중 오류가 발생했습니다: " + e.getMessage() +
                            " (Received ID Token Length: " + idTokenString.length() + ")");
        }
    }

}
