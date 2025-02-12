package com.edison.project.global.config;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.domain.member.dto.MemberResponseDto;
import com.edison.project.domain.member.service.CustomOidcUserService;
import com.edison.project.domain.member.service.MemberService;
import com.edison.project.global.security.CustomAuthenticationEntryPoint;
import com.edison.project.global.security.CustomUserPrincipal;
import com.edison.project.global.security.JwtAuthenticationFilter;
import com.edison.project.global.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.List;

import static com.edison.project.common.status.SuccessStatus._OK;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final MemberService memberService;
    private final CustomOidcUserService customOidcUserService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final JwtUtil jwtUtil;
  
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/.well-known/acme-challenge/**").permitAll() // 서버 인증서 관련 경로 추가
                        .requestMatchers("/members/refresh").permitAll()
                        .requestMatchers("/members/google").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // OIDC 전용 (구글로그인)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOidcUserService))
                        .successHandler(this::oidcLoginSuccessHandler)
                        .failureHandler(this::oidcLoginFailureHandler)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint) // EntryPoint 등록
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

        MemberResponseDto.LoginResultDto dto = memberService.generateTokensForOidcUser(email);

        // SecurityContextHolder에 인증 정보 설정
        Long userId = jwtUtil.extractUserId(dto.getAccessToken());
        CustomUserPrincipal customUserPrincipal = new CustomUserPrincipal(userId, email);

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                customUserPrincipal,
                dto.getAccessToken(),
                List.of(new SimpleGrantedAuthority("USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        ResponseEntity<ApiResponse> apiResponse = ApiResponse.onSuccess(_OK, dto);

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