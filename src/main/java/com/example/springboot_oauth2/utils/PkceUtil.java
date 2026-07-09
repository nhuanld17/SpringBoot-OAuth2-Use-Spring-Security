package com.example.springboot_oauth2.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * PKCE (Proof Key for Code Exchange) - RFC 7636.
 *
 * Y tuong: truoc khi redirect sang Google, client tao ra 1 chuoi bi mat ngau nhien
 * "code_verifier" (chi client biet), roi gui di ban "bam" cua no "code_challenge".
 * Luc doi code lay token, client phai chung minh minh biet code_verifier goc.
 *
 * -> Neu ke tan cong cuop duoc authorization code, ho VAN khong doi duoc token
 *    vi khong biet code_verifier.
 */
public final class PkceUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    // Base64 URL-safe, KHONG padding (yeu cau cua chuan PKCE)
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private PkceUtil() {
    }

    /**
     * Sinh code_verifier: chuoi ngau nhien 32 byte -> base64url (~43 ky tu).
     */
    public static String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    /**
     * code_challenge = BASE64URL( SHA-256( code_verifier ) ).
     * code_challenge_method = S256.
     */
    public static String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return URL_ENCODER.encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 khong kha dung", e);
        }
    }

    /**
     * Sinh gia tri "state" ngau nhien de chong CSRF o buoc callback.
     */
    public static String generateState() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }
}
