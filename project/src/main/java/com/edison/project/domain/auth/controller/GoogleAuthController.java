package com.edison.project.domain.auth.controller;

import com.auth0.jwt.interfaces.Payload;
import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.auth.dto.GoogleAuthRequestDto;
import com.edison.project.domain.auth.dto.TokenResponseDto;
import com.edison.project.domain.auth.service.TokenService;
import com.edison.project.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    @PostMapping("/google")
    public ResponseEntity<ApiResponse> googleLogin(@RequestBody GoogleAuthRequestDto requestDto) {
        String idToken = requestDto.getIdToken();
        String email = requestDto.getEmail();

        if (idToken == null || idToken.isEmpty() || email == null || email.isEmpty()) {
            return ApiResponse.onFailure(ErrorStatus._BAD_REQUEST);
        }
        com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload payload = jwtUtil.verifyGoogleIdToken(idToken);
        if (payload == null || !email.equals(email)) {
            return ApiResponse.onFailure(ErrorStatus._UNAUTHORIZED);
        }

        // ✅ Access Token 및 Refresh Token 생성
        TokenResponseDto tokenResponse = tokenService.generateTokens(email);

        return ApiResponse.onSuccess(SuccessStatus._OK, tokenResponse);
    }
}
