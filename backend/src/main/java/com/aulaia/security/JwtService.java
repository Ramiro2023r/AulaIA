package com.aulaia.security;

import com.aulaia.entity.Rol;
import com.aulaia.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Generación, validación y extracción de tokens JWT (Prompt 2.3).
 *
 * <p>Claims del token:
 * <ul>
 *   <li>{@code sub} → username</li>
 *   <li>{@code userId} → id del usuario</li>
 *   <li>{@code rol} → rol (ADMIN | DOCENTE)</li>
 *   <li>{@code iat} / {@code exp} → emisión y expiración</li>
 * </ul>
 * Nunca incluye passwordHash ni información personal/académica.
 *
 * <p>Firma: HMAC-SHA256 (HS256) con {@code jwt.secret} (JWT_SECRET).
 * La clave debe tener al menos 32 bytes (256 bits); si es más corta o no
 * está configurada, el bean falla al arrancar (fail fast, sin degradar
 * la seguridad silenciosamente).
 *
 * <p>Seguridad: los tokens NUNCA se registran en logs. Este servicio no
 * consulta la base de datos (la comprobación de usuario activo se hará
 * fuera, con datos persistentes, en la integración con el filtro).
 *
 * <p>Comportamiento ante tokens inválidos (sin stack traces al cliente):
 * los métodos {@code extract*} lanzan {@link JwtException} controlada;
 * {@link #isTokenValid} y {@link #isTokenExpired} devuelven {@code false}
 * salvo que la expiración sea la causa.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** Longitud mínima del secreto para HS256 (256 bits = 32 bytes). */
    static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final Duration expiration;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms}") long expirationMs) {
        byte[] secretBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET (jwt.secret) debe tener al menos 32 caracteres para HS256; "
                            + "configúrala mediante variable de entorno (nunca en el repositorio).");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.expiration = Duration.ofMillis(expirationMs);
        log.info("JwtService inicializado (HS256, expiración {} ms)", expirationMs);
    }

    /**
     * Genera un token con la expiración configurada (jwt.expiration-ms).
     */
    public String generateToken(Usuario usuario) {
        return generateToken(usuario, expiration);
    }

    /**
     * Genera un token con expiración explícita (útil para pruebas
     * controladas de expiración sin sleeps).
     */
    public String generateToken(Usuario usuario, Duration expiration) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim("userId", usuario.getId())
                .claim("rol", usuario.getRol().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    public Rol extractRole(String token) {
        return Rol.valueOf(extractAllClaims(token).get("rol", String.class));
    }

    /**
     * Valida firma, expiración y que el username coincida con el esperado.
     */
    public boolean isTokenValid(String token, String expectedUsername) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getSubject().equals(expectedUsername)
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            extractAllClaims(token);
            return false;
        } catch (ExpiredJwtException ex) {
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}