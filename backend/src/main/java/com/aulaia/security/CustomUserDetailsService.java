package com.aulaia.security;

import com.aulaia.entity.Rol;
import com.aulaia.entity.Usuario;
import com.aulaia.repository.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carga de usuarios desde {@code usuarios} (tabla PostgreSQL) para Spring
 * Security (Prompt 2.4).
 *
 * <p>Comportamiento:
 * <ul>
 *   <li>Busca por username; si no existe → {@link UsernameNotFoundException}.</li>
 *   <li>Usuarios inactivos ({@code activo=false}) → también
 *       {@link UsernameNotFoundException}: se tratan como inexistentes para
 *       NO revelar el estado de la cuenta y revocar el acceso inmediatamente
 *       (el JWT por sí solo no basta).</li>
 *   <li>Rol mapeado a authority: {@code ADMIN → ROLE_ADMIN},
 *       {@code DOCENTE → ROLE_DOCENTE}.</li>
 * </ul>
 *
 * <p>{@code passwordHash} se usa como credential interna de Spring Security
 * (requerida por {@link User}) y nunca se registra en logs (el
 * {@code toString} de {@link User} lo enmascara como {@code [PROTECTED]}).
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        if (!usuario.isActivo()) {
            throw new UsernameNotFoundException("Usuario inactivo: " + username);
        }

        return User.withUsername(usuario.getUsername())
                .password(usuario.getPasswordHash())
                .authorities(mapRolToAuthority(usuario.getRol()))
                .build();
    }

    private GrantedAuthority mapRolToAuthority(Rol rol) {
        return new SimpleGrantedAuthority("ROLE_" + rol.name());
    }
}