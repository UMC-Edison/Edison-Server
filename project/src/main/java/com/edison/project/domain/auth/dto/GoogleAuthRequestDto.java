package com.edison.project.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoogleAuthRequestDto {
    private String idToken;
    private String email;
}
