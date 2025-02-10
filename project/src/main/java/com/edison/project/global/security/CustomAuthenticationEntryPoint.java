package com.edison.project.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("isSuccess", false);
        errorResponse.put("code", "LOGIN4000");
        errorResponse.put("message", "커스텀 엔트리 예외입니다.");

        // 응답 설정
        response.setContentType("application/json;charset=UTF-8"); // Content-Type + UTF-8 설정
        response.setCharacterEncoding("UTF-8"); // 문자 인코딩 설정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 상태 코드

        // JSON 응답 반환
        objectMapper.writeValue(response.getWriter(), errorResponse);

        return;
    }
}
