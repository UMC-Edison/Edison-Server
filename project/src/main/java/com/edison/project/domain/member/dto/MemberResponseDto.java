package com.edison.project.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MemberResponseDto {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResultDto{
        String accessToken;
        String refreshToken;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileResultDto{
        private String nickname;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProfileResultDto {

        @NotBlank(message = "닉네임은 필수입니다.")
        private String nickname;

        private String imageUrl;
    }
  
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshResultDto{
        String accessToken;
    }
}
