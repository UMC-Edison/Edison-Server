package com.edison.project.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    private String email;

    @Column(nullable = false)
    private String refreshToken;

    public static RefreshToken create(String email, String refreshToken) {
        return new RefreshToken(email, refreshToken);
    }

    public void updateToken(String newToken) {
        this.refreshToken = newToken;
    }
}

