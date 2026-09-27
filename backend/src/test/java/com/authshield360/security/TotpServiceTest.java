package com.authshield360.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** TOTP correctness (UC-02 / UC-09). */
class TotpServiceTest {

    private final TotpService totpService = new TotpService();

    @Test
    void generatedCodeVerifiesAgainstItsOwnSecret() {
        String secret = totpService.generateSecret();
        String code = totpService.currentCode(secret);
        assertThat(code).hasSize(6);
        assertThat(totpService.verify(secret, code)).isTrue();
    }

    @Test
    void wrongCodeIsRejected() {
        String secret = totpService.generateSecret();
        String code = totpService.currentCode(secret);
        String wrong = code.equals("000000") ? "111111" : "000000";
        assertThat(totpService.verify(secret, wrong)).isFalse();
    }

    @Test
    void anotherSecretCannotVerifyTheCode() {
        String secretA = totpService.generateSecret();
        String secretB = totpService.generateSecret();
        String codeA = totpService.currentCode(secretA);
        assertThat(totpService.verify(secretB, codeA)).isFalse();
    }

    @Test
    void otpauthUriContainsSecretAndIssuer() {
        String secret = totpService.generateSecret();
        String uri = totpService.otpauthUri(secret, "student01", "AuthShield 360");
        assertThat(uri).startsWith("otpauth://totp/").contains(secret).contains("issuer=");
    }

    @Test
    void base32RoundTrip() {
        byte[] original = "AuthShield360".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String encoded = TotpService.base32Encode(original);
        assertThat(TotpService.base32Decode(encoded)).isEqualTo(original);
    }
}
