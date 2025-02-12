package com.edison.project.global.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {
    private final Object principal;
    private final Object credentials;
    private final String token;

    // 인증되지 않은 상태
    public JwtAuthenticationToken(String token) {
        super(null);
        this.token = token;
        this.principal = null;
        this.credentials = null;
        setAuthenticated(false);
    }

    // 인증 완료된 상태
    public JwtAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        this.token = credentials.toString();
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public String getToken() {
        return token;
    }
}
