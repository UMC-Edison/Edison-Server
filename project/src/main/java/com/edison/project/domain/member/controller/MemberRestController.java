package com.edison.project.domain.member.controller;

import com.edison.project.common.response.Response;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.member.dto.MemberRequestDto;
import com.edison.project.domain.member.dto.MemberResponseDto;
import com.edison.project.domain.member.service.MemberService;
import com.edison.project.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Swagger/OpenAPI
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
@Tag(name = "Member", description = "회원(사용자) 관련 API")
public class MemberRestController {

    private final MemberService memberService;

    // 회원정보 변경
    @Operation(summary = "회원 정보 변경", description = "사용자의 프로필 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @PatchMapping("/profile")
    public ResponseEntity<Response> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal, @RequestBody MemberRequestDto.UpdateProfileDto request) {
        return memberService.updateProfile(userPrincipal, request);
    }

    // 회원정보 조회
    @Operation(summary = "회원 정보 조회", description = "현재 인증된 사용자의 프로필 정보를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = MemberResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> getProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        return memberService.getProfile(userPrincipal);
    }


    @Operation(summary = "아이덴티티 키워드 조회", description = "사용자의 아이덴티티 키워드를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = MemberResponseDto.IdentityKeywordsResultDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @GetMapping("/identity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> getIdentityKeywords(@Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        MemberResponseDto.IdentityKeywordsResultDto result = memberService.getIdentityKeywords(userPrincipal);
        return Response.onSuccess(SuccessStatus._OK, result);
    }

    @Operation(summary = "아이덴티티 테스트 저장", description = "사용자의 아이덴티티 테스트 결과를 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = MemberResponseDto.IdentityTestSaveResultDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @PatchMapping("/identity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> updateIdentityTest(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestBody @Valid MemberRequestDto.IdentityTestSaveDto request) {
        MemberResponseDto.IdentityTestSaveResultDto result = memberService.updateIdentityTest(userPrincipal, request);
        return Response.onSuccess(SuccessStatus._OK, result);
    }



    @Operation(summary = "로그아웃", description = "현재 사용자 로그아웃 처리")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> logout(@Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        return memberService.logout(userPrincipal);
    }

    @Operation(summary = "토큰 리프레시", description = "리프레시 토큰으로 액세스 토큰을 재발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 토큰", content = @Content)
    })
    @PostMapping("/refresh")
    public ResponseEntity<Response> refreshAccessToken(@RequestHeader("Refresh-Token") String refreshToken) {
        return memberService.refreshAccessToken(refreshToken);
    }

    @Operation(summary = "회원 탈퇴", description = "현재 사용자를 탈퇴 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @DeleteMapping("/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> cancel(@Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        return memberService.cancel(userPrincipal);
    }

    @Operation(summary = "구글 로그인", description = "구글 ID 토큰으로 로그인 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = MemberResponseDto.LoginResultDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 토큰", content = @Content)
    })
    @PostMapping("/google/login")
    public ResponseEntity<Response> googleLogin(@RequestBody MemberRequestDto.GoogleLoginDto request) {
        MemberResponseDto.LoginResultDto dto = memberService.processGoogleLogin(request.getIdToken());
        return Response.onSuccess(SuccessStatus._OK, dto);
    }

    @Operation(summary = "구글 회원가입", description = "구글 ID 토큰으로 회원가입 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = MemberResponseDto.SignupResultDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 토큰", content = @Content)
    })
    @PostMapping("/google/signup")
    public ResponseEntity<Response> googleSignup(@Valid @RequestBody MemberRequestDto.GoogleSignupDto request) {
        MemberResponseDto.SignupResultDto dto = memberService.processGoogleSignup(request.getIdToken(), request.getNickname(), request.getIdentities());
        return Response.onSuccess(SuccessStatus._OK, dto);
    }

    @Operation(summary = "사용자 아이덴티티 키워드 포맷 조회", description = "사용자 소속 아이덴티티 키워드들을 포맷된 문자열로 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @GetMapping("/spaces/identity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> getCategorizedIdentityKeywords(@Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        String formattedResult = memberService.getCategorizedIdentityKeywords(userPrincipal);
        return Response.onSuccess(SuccessStatus._OK, formattedResult);
    }

}