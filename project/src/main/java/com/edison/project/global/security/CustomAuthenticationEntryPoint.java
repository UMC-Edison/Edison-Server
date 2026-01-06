package com.edison.project.global.security;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.ErrorStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {


        logger.debug("CustomAuthenticationEntryPoint - commence 호출됨");
        logger.debug("요청 URI: {}", request.getRequestURI());
        logger.debug("요청 메서드: {}", request.getMethod());
        logger.debug("클라이언트 IP: {}", request.getRemoteAddr());

        if (authException != null) {
            logger.error("인증 예외 발생: {}", authException.getMessage(), authException);
        }

        ResponseEntity<ApiResponse> errorResponse = ApiResponse.onFailure(ErrorStatus.CUSTOM_ENTRY_EXCEPTION);

        // HttpServletResponse에 직접 작성
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(errorResponse.getStatusCode().value());

        // JSON 응답 반환
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(response.getWriter(), errorResponse.getBody());

        logger.debug("에러 응답 전송 완료, 상태 코드: {}", response.getStatus());
    }
}
