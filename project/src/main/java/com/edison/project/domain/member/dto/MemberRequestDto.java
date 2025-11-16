package com.edison.project.domain.member.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class MemberRequestDto {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProfileDto {

        @NotBlank
        private String nickname;

        private String imageUrl;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdentityTestSaveDto {

        @NotBlank(message = "카테고리는 필수입니다.")
        private String category;

        @NotEmpty(message = "키워드는 최소 하나 이상 선택해야 합니다.")
        @Size(max = 5, message = "키워드는 최대 5개까지 선택할 수 있습니다.")
        private List<Integer> keywords;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoogleSignupDto{

        @NotBlank
        private String idToken;

        @NotBlank
        private String nickname;

        @NotNull
        @Valid
        private MemberRequestDto.IdentityTestSaveDto identity;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoogleLoginDto{

        @NotBlank
        private String idToken;

        private String nickname;

        @Valid
        private MemberRequestDto.IdentityTestSaveDto identity;
    }
}
