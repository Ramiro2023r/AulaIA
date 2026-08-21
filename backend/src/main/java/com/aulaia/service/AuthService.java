package com.aulaia.service;

import com.aulaia.dto.auth.AuthenticatedUserResponse;
import com.aulaia.dto.auth.LoginRequest;
import com.aulaia.dto.auth.LoginResponse;
import com.aulaia.entity.Usuario;
import com.aulaia.exception.InvalidCredentialsException;
import com.aulaia.repository.UsuarioRepository;
import com.aulaia.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Autenticación de usuarios (login, Prompt 2.5).
 *
 * <p>Anti-enumeración: username inexistente, password incorrecta y
 * usuario inactivo producen EXACTAMENTE la misma respuesta externa
 * (401 INVALID_CREDENTIALS). Para igualar el costo de BCrypt en
 * usuarios inexistentes se ejecuta un {@code matches()} contra un hash
 * ficticio ({@link #DUMMY_PASSWORD_HASH}); ese hash NO es una credencial
 * real de ningún usuario.
 *
 * <p>Logging: se registra "login exitoso" (con username) y "login fallido"
 * (sin username, sin password ni hashes).
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /**
     * Hash BCrypt de una contraseña aleatoria desechable, usado solo para
     * que la comparación de timing en usuarios inexistentes sea similar a
     * la de usuarios reales. Nunca corresponde a una cuenta real.
     */
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$9FBCvcLTf57czzCFTRkbhOjTaoLx4gipas9jSfjMR6pq1vb9eJHAu";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long expiresInSeconds;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Value("${jwt.expiration-ms}") long expirationMs) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.expiresInSeconds = expirationMs / 1000;
    }

    /**
     * Valida credenciales contra la tabla {@code usuarios} y devuelve un
     * token Bearer JWT. Actualiza {@code ultimoLoginAt} (hora del
     * servidor) solo si el login es exitoso.
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(request.username()).orElse(null);

        if (usuario == null) {
            passwordEncoder.matches(request.password(), DUMMY_PASSWORD_HASH);
            log.warn("Login fallido");
            throw new InvalidCredentialsException();
        }

        if (!usuario.isActivo()) {
            passwordEncoder.matches(request.password(), usuario.getPasswordHash());
            log.warn("Login fallido");
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            log.warn("Login fallido");
            throw new InvalidCredentialsException();
        }

        usuario.setUltimoLoginAt(OffsetDateTime.now());
        usuarioRepository.save(usuario);

        String accessToken = jwtService.generateToken(usuario);
        log.info("Login exitoso: {}", usuario.getUsername());

        return LoginResponse.of(accessToken, expiresInSeconds,
                new AuthenticatedUserResponse(usuario.getId(), usuario.getUsername(), usuario.getRol()));
    }
}