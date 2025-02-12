package com.edison.project.test;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.util.Collections;

public class TokenVerifierTest {
    private static final String CLIENT_ID = "number~~.apps.googleusercontent.com";

    public static void main(String[] args) {
        String idTokenString = "idtokentoken"; // 테스트할 토큰

        if (idTokenString == null || idTokenString.isEmpty()) {
            System.err.println("❌ ID Token is null or empty");
            return;
        }

        System.out.println("🔍 Received ID Token: " + idTokenString);

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(CLIENT_ID))
                .build();

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();

                System.out.println("✅ Google ID Token is valid!");
                System.out.println("Issuer: " + payload.getIssuer());
                System.out.println("Email: " + payload.getEmail());
                System.out.println("Email Verified: " + payload.getEmailVerified());
                System.out.println("Name: " + payload.get("name"));
                System.out.println("Picture URL: " + payload.get("picture"));
                System.out.println("Expiration Time: " + payload.getExpirationTimeSeconds());
            } else {
                System.out.println("❌ Invalid Google ID Token");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error verifying Google ID Token: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
