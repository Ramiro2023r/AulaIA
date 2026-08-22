package com.aulaia.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de seguridad de AulaIA (Sprint 2).
 *
 * <p>Decisiones:
 * <ul>
 *   <li><b>CSRF deshabilitado</b>: la API es stateless con autenticación
 *       Bearer JWT. Sin sesiones HTTP ni cookies, el token CSRF no aplica
 *       (docs/07-PLAN_EJECUCION: "CSRF según arquitectura").</li>
 *   <li><b>SessionCreationPolicy.STATELESS</b>: nunca se crean sesiones
 *       HTTP; cada solicitud se autentica por su token.</li>
 *   <li><b>JwtAuthenticationFilter</b> registrado antes de
 *       {@code UsernamePasswordAuthenticationFilter}: autentica requests
 *       con {@code Authorization: Bearer <token>}.</li>
 *   <li><b>401/403 JSON</b>: {@link RestAuthenticationEntryPoint},
 *       {@link RestAccessDeniedHandler} y el filtro JWT devuelven
 *       {@code ApiErrorResponse} consistente (UNAUTHORIZED / FORBIDDEN).</li>
 *   <li><b>PasswordEncoder BCrypt</b>: nunca contraseñas planas; se usará
 *       en el login (próximo prompt).</li>
 * </ul>
 *
 * <p>Rutas públicas: documentación (OpenAPI/Swagger), salud de Actuator y
 * /test/** (controladores SOLO de pruebas MockMvc; no existen rutas /test
 * en producción). El resto requiere autenticación.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers("/test/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:4200", "http://localhost:*")); // Angular local dev
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "X-AI-Provider", "X-AI-Key"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}