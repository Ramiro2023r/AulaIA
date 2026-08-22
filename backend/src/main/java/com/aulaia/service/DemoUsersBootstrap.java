package com.aulaia.service;

import com.aulaia.entity.Docente;
import com.aulaia.entity.Rol;
import com.aulaia.entity.Usuario;
import com.aulaia.repository.DocenteRepository;
import com.aulaia.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea las cuentas mínimas para demostraciones locales.
 * Solo se registra en el perfil {@code dev}; nunca se ejecuta en producción.
 */
@Component
@Profile("dev")
public class DemoUsersBootstrap implements ApplicationRunner {

    static final String ADMIN_USERNAME = "admin";
    static final String ADMIN_PASSWORD = "Admin12345678!";
    static final String DOCENTE_USERNAME = "docente@aulaia.com";
    static final String DOCENTE_PASSWORD = "123456";

    private static final Logger log = LoggerFactory.getLogger(DemoUsersBootstrap.class);

    private final UsuarioRepository usuarioRepository;
    private final DocenteRepository docenteRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public DemoUsersBootstrap(UsuarioRepository usuarioRepository,
                              DocenteRepository docenteRepository,
                              PasswordEncoder passwordEncoder,
                              @Value("${app.demo-users.enabled:true}") boolean enabled) {
        this.usuarioRepository = usuarioRepository;
        this.docenteRepository = docenteRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        crearAdminSiNoExiste();
        crearDocenteSiNoExiste();
    }

    private void crearAdminSiNoExiste() {
        if (usuarioRepository.existsByUsername(ADMIN_USERNAME)) {
            log.info("Usuario demo ADMIN ya existe");
            return;
        }

        Usuario admin = crearUsuario(ADMIN_USERNAME, ADMIN_PASSWORD, Rol.ADMIN);
        usuarioRepository.save(admin);
        log.info("Usuario demo ADMIN creado");
    }

    private void crearDocenteSiNoExiste() {
        if (usuarioRepository.existsByUsername(DOCENTE_USERNAME)) {
            log.info("Usuario demo DOCENTE ya existe");
            return;
        }

        Usuario usuarioDocente = usuarioRepository.save(
                crearUsuario(DOCENTE_USERNAME, DOCENTE_PASSWORD, Rol.DOCENTE));

        Docente docente = new Docente();
        docente.setUsuario(usuarioDocente);
        docente.setNombres("Docente");
        docente.setApellidos("Prueba");
        docente.setCorreoAlternativo(DOCENTE_USERNAME);
        docente.setActivo(true);
        docenteRepository.save(docente);
        log.info("Usuario demo DOCENTE creado");
    }

    private Usuario crearUsuario(String username, String password, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setRol(rol);
        usuario.setActivo(true);
        return usuario;
    }
}
