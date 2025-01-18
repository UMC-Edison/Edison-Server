package com.edison.project.global.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CustomUserPrincipal {
    private Long memberId;
    private String email;
}
