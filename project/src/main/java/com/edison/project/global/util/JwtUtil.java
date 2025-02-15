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
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;

@Component
public class JwtUtil {
    private static final long DEFAULT_TTL = 1;
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private static final String GOOGLE_ISSUER = "https://accounts.google.com";
    
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId ;

    public String generateAccessToken(Long memberId, String email) {
        return JWT.create()
                .withSubject(String.valueOf(memberId))
                .withClaim("email", email)
                .withExpiresAt(new Date(System.currentTimeMillis() + accessTokenExpiration * 1000))
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
        } catch (TokenExpiredException e) {
            // 토큰이 만료된 경우
            throw new GeneralException(ErrorStatus.ACCESSTOKEN_EXPIRED);
        }catch (Exception e) {
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
            Date expiration = decodedJWT.getExpiresAt(); // 만료 시간 가져오기
            return expiration.getTime() - System.currentTimeMillis(); // 잔여 시간 계산
            
        } catch (TokenExpiredException e) {
            // 토큰이 만료된 경우
            return 1;
        }catch (JWTVerificationException e) {
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
        } catch (TokenExpiredException e) {
            // 토큰이 만료된 경우
            return true;
        } catch (JWTVerificationException e) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }
    }

    public GoogleIdToken.Payload verifyGoogleIdToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory()
            )

                    .setAudience(Collections.singletonList(clientId)) // 내 앱의 Client ID
                    .setIssuer(GOOGLE_ISSUER) // Google이 발급한 토큰인지 확인
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                return idToken.getPayload(); // 사용자 정보 추출
            } else {
                throw new GeneralException(ErrorStatus.INVALID_TOKEN);
            }
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }
    }
}
