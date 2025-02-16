package com.edison.project.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class MemberResponseDto {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResultDto{

        private AtomicReference<Boolean> isNewMember;
        private Long memberId;
        private String email;
        private String accessToken;
        private String refreshToken;
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
        private String accessToken;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdentityTestSaveResultDto {
        private String category;
        private List<Integer> keywords;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdentityKeywordDto {
        private Integer keywordId;
        private String keywordName;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdentityKeywordsResultDto {
        private Map<String, List<IdentityKeywordDto>> categories;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberResultDto {

        private String email;

        @NotBlank(message = "닉네임은 필수입니다.")
        private String nickname;

        private String profileImg;
    }
}
