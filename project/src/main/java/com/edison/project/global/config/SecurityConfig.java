package com.edison.project.global.config;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.domain.member.dto.MemberResponseDto;
import com.edison.project.domain.member.service.CustomOidcUserService;
import com.edison.project.domain.member.service.MemberService;
import com.edison.project.global.security.CustomAuthenticationEntryPoint;
import com.edison.project.global.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

import static com.edison.project.common.status.SuccessStatus._OK;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final MemberService memberService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOidcUserService customOidcUserService;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        //로그인 없이 접근 가능
                        .requestMatchers("/.well-known/acme-challenge/**").permitAll()
                        .requestMatchers("/members/google/login","/members/google/signup", "/favicon.ico").permitAll()
                        .requestMatchers(HttpMethod.GET, "/artletters").permitAll() // 전체 아트레터 조회
                        .requestMatchers(HttpMethod.GET, "/artletters/search").permitAll() // 검색 API
                        .requestMatchers(HttpMethod.GET, "/artletters/**").permitAll() //특정 아트레터 조회
                        .requestMatchers(HttpMethod.GET, "/artletters/recommend-bar/category").permitAll() // 추천 카테고리
                        .requestMatchers(HttpMethod.GET, "/artletters/recommend-bar/keyword").permitAll() // 추천 키워드
                        .requestMatchers(HttpMethod.POST, "/artletters/editor-pick").permitAll()
                        .requestMatchers(HttpMethod.POST, "/spaces/generate").permitAll()
                        .requestMatchers("/s3/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/artletters/more").permitAll()
                        .requestMatchers("/identity/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
//                .oauth2Login(oauth2 -> oauth2
//                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(customOidcUserService))
//                        .successHandler(this::oidcLoginSuccessHandler)
//                        .failureHandler(this::oidcLoginFailureHandler)
//                )
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(customAuthenticationEntryPoint)
                );

        return http.build();
    }

    // OIDC 로그인 성공 핸들러
    private void oidcLoginSuccessHandler(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        if (!(authentication.getPrincipal() instanceof OidcUser)) {
            sendErrorResponse(response, ErrorStatus._UNAUTHORIZED);
            return;
        }

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String email = oidcUser.getEmail();

        //JWT 토큰 발급
        MemberResponseDto.LoginResultDto dto = memberService.generateTokensForOidcUser(email);

        // 성공 응답 반환
        sendSuccessResponse(response, dto);
    }

    // OIDC 로그인 실패 핸들러
    private void oidcLoginFailureHandler(HttpServletRequest request,
                                         HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {
        if ("access_denied".equals(request.getParameter("error"))) {
            sendErrorResponse(response, ErrorStatus.LOGIN_CANCELLED);
        } else {
            sendErrorResponse(response, ErrorStatus._BAD_REQUEST);
        }
    }

    // 인증 실패 응답 처리
    private void sendErrorResponse(HttpServletResponse response, ErrorStatus errorStatus) throws IOException {
        ResponseEntity<ApiResponse> apiResponse = ApiResponse.onFailure(errorStatus);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(apiResponse.getStatusCode().value());
        new ObjectMapper().writeValue(response.getWriter(), apiResponse.getBody());
    }

    // 인증 성공 응답 처리
    private void sendSuccessResponse(HttpServletResponse response, MemberResponseDto.LoginResultDto dto) throws IOException {
        ResponseEntity<ApiResponse> apiResponse = ApiResponse.onSuccess(_OK, dto);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        new ObjectMapper().writeValue(response.getWriter(), apiResponse.getBody());
    }

    // 인증 매니저
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // 비밀번호 암호화
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
