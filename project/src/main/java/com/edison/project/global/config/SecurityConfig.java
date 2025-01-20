package com.edison.project.global.config;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.domain.member.service.CustomOidcUserService;
import com.edison.project.domain.member.service.MemberServiceImpl;
import com.edison.project.global.security.CustomAuthenticationEntryPoint;
import com.edison.project.global.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final MemberServiceImpl memberService;
    private final CustomOidcUserService customOidcUserService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
  
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/members/register").authenticated()
                        .requestMatchers("/favicon.ico").permitAll()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint) // EntryPoint 등록
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // OIDC 전용 (구글로그인)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOidcUserService))
                        .successHandler(this::oidcLoginSuccessHandler)
                        .failureHandler(this::oidcLoginFailureHandler)
                );

        return http.build();
    }

    // OIDC 로그인 성공 핸들러
    private void oidcLoginSuccessHandler(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        if (!(authentication.getPrincipal() instanceof OidcUser)){
            ResponseEntity<ApiResponse> apiResponse = ApiResponse.onFailure(ErrorStatus._UNAUTHORIZED);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(apiResponse.getStatusCode().value());

            new ObjectMapper().writeValue(response.getWriter(), apiResponse.getBody());
        }


        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String email = oidcUser.getEmail();

        ResponseEntity<ApiResponse> apiResponse = memberService.generateTokensForOidcUser(email);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        new ObjectMapper().writeValue(response.getWriter(), apiResponse.getBody());

    }

    private void oidcLoginFailureHandler(HttpServletRequest request,
                                         HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {

        if ("access_denied".equals(request.getParameter("error"))) {
            ResponseEntity<ApiResponse> apiResponse = ApiResponse.onFailure(ErrorStatus.LOGIN_CANCELLED);

            // JSON 에러 메시지 반환
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(apiResponse.getStatusCode().value());

            // JSON 형식으로 클라이언트에 전달
            new ObjectMapper().writeValue(response.getWriter(), apiResponse.getBody());
            return;
        }

        ResponseEntity<ApiResponse> apiResponse = ApiResponse.onFailure(ErrorStatus._BAD_REQUEST);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(apiResponse.getStatusCode().value());

        new ObjectMapper().writeValue(response.getWriter(), apiResponse.getBody());
    }

    //인증 매니저
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    //비밀번호 암호화
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}