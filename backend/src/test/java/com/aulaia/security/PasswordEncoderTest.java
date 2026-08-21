package com.aulaia.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias del {@link PasswordEncoder} (BCrypt, Prompt 2.2).
 */
class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void codificaUnaPassword() {
        String hash = passwordEncoder.encode("s3cr3t-password");

        assertThat(hash).isNotBlank().isNotEqualTo("s3cr3t-password");
        assertThat(hash).startsWith("$2");
    }

    @Test
    void hashDistintoDeLaPasswordOriginal() {
        String hash = passwordEncoder.encode("mi-password");

        assertThat(hash).doesNotContain("mi-password");
    }

    @Test
    void matchesCorrecto() {
        String hash = passwordEncoder.encode("s3cr3t-password");

        assertThat(passwordEncoder.matches("s3cr3t-password", hash)).isTrue();
    }

    @Test
    void passwordIncorrectaNoHaceMatch() {
        String hash = passwordEncoder.encode("s3cr3t-password");

        assertThat(passwordEncoder.matches("password-incorrecta", hash)).isFalse();
    }

    @Test
    void hashesDiferentesParaLaMismaPasswordPorSaltAleatorio() {
        String hash1 = passwordEncoder.encode("misma-password");
        String hash2 = passwordEncoder.encode("misma-password");

        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(passwordEncoder.matches("misma-password", hash1)).isTrue();
        assertThat(passwordEncoder.matches("misma-password", hash2)).isTrue();
    }
}