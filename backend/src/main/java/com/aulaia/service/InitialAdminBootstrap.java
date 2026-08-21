package com.aulaia.service;

import com.aulaia.entity.Rol;
import com.aulaia.entity.Usuario;
import com.aulaia.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Creación segura y controlada del primer administrador de una
 * instalación (Prompt 2.6).
 *
 * <p>Se ejecuta después de que DataSource, Flyway y JPA estén listos
 * ({@link ApplicationRunner}). Flujo:
 * <ol>
 *   <li>Si {@code aulaia.bootstrap.admin.enabled} es {@code false}
 *       (default en TODOS los perfiles): no hace nada.</li>
 *   <li>Si está habilitado y ya existe algún ADMIN: finaliza de forma
 *       idempotente (no crea duplicados).</li>
 *   <li>Si no existe ADMIN: valida username/password, guarda el admin con
 *       BCrypt, rol ADMIN, activo=true y ultimoLoginAt=null.</li>
 * </ol>
 *
 * <p>Seguridad:
 * <ul>
 *   <li>Username y password vienen SOLO del entorno
 *       ({@code AULAIA_BOOTSTRAP_ADMIN_*}); sin fallback real.</li>
 *   <li>Variables inválidas (blank, username &gt; 100, password &lt; 12)
 *       con bootstrap habilitado → el ARRANQUE FALLA (fail fast); nunca se
 *       crea un admin parcialmente configurado.</li>
 *   <li>Si el username ya pertenece a otro usuario (p. ej. DOCENTE), el
 *       bootstrap falla SIN modificar esa cuenta ni cambiar su rol.</li>
 *   <li>Nunca se registran en logs: password, hash, JWT ni secretos.</li>
 * </ul>
 */
@Component
public class InitialAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminBootstrap.class);

    /** Longitud mínima razonable para la contraseña del admin inicial. */
    static final int MIN_PASSWORD_LENGTH = 12;

    private static final int MAX_USERNAME_LENGTH = 100;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String username;
    private final String password;

    public InitialAdminBootstrap(UsuarioRepository usuarioRepository,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${aulaia.bootstrap.admin.enabled:false}") boolean enabled,
                                 @Value("${aulaia.bootstrap.admin.username:}") String username,
                                 @Value("${aulaia.bootstrap.admin.password:}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Bootstrap de administrador deshabilitado");
            return;
        }

        String trimmedUsername = validateAndTrimUsername();
        validatePassword();

        if (usuarioRepository.existsByRol(Rol.ADMIN)) {
            log.info("Ya existe un administrador; bootstrap omitido");
            return;
        }

        if (usuarioRepository.existsByUsername(trimmedUsername)) {
            throw new IllegalStateException(
                    "El username configurado en AULAIA_BOOTSTRAP_ADMIN_USERNAME ya existe con otro rol; "
                            + "no se modificará. Bootstrap de administrador abortado.");
        }

        Usuario admin = new Usuario();
        admin.setUsername(trimmedUsername);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRol(Rol.ADMIN);
        admin.setActivo(true);

        usuarioRepository.save(admin);
        log.info("Administrador inicial creado: {}", trimmedUsername);
    }

    private String validateAndTrimUsername() {
        if (!StringUtils.hasText(username)) {
            throw new IllegalStateException(
                    "AULAIA_BOOTSTRAP_ADMIN_USERNAME es obligatorio cuando el bootstrap está habilitado.");
        }
        String trimmed = username.trim();
        if (trimmed.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalStateException(
                    "AULAIA_BOOTSTRAP_ADMIN_USERNAME no puede exceder " + MAX_USERNAME_LENGTH + " caracteres.");
        }
        return trimmed;
    }

    private void validatePassword() {
        if (!StringUtils.hasText(password)) {
            throw new IllegalStateException(
                    "AULAIA_BOOTSTRAP_ADMIN_PASSWORD es obligatoria cuando el bootstrap está habilitado.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "AULAIA_BOOTSTRAP_ADMIN_PASSWORD debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }
    }
}