package com.edison.project.test;

import java.util.Base64;

public class TokenValidator {
    public static String decodeBase64Url(String encodedString) {
        try {
            return new String(Base64.getUrlDecoder().decode(encodedString));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("🔴 Base64 URL Decoding 실패: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        String idToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImVlYzUzNGZhNWI4Y2FjYTIwMWNhOGQwZmY5NmI1NGM1NjIyMTBkMWUiLCJ0eXAiOiJKV1QifQ..";

        String[] tokenParts = idToken.split("\\.");

        if (tokenParts.length != 3) {
            System.out.println("🔴 JWT 형식이 올바르지 않습니다!");
        } else {
            System.out.println("✅ Header Decoded: " + decodeBase64Url(tokenParts[0]));
            System.out.println("✅ Payload Decoded: " + decodeBase64Url(tokenParts[1]));
        }
    }
}
