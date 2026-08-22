package com.aulaia.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Configuración base para pruebas de integración con Testcontainers.
 * <p>
 * Proporciona un contenedor PostgreSQL real para tests que lo necesiten.
 * Se activa con el perfil {@code itest} o {@code testcontainers}.
 * 
 * <p>Uso:
 * <pre>
 * @SpringBootTest
 * @Testcontainers
 * @ActiveProfiles("testcontainers")
 * class MiIntegracionTest extends TestcontainersConfig { ... }
 * </pre>
 */
@Testcontainers
@Configuration
@Profile("testcontainers")
public class TestcontainersConfig {

    /** Contenedor PostgreSQL compartido para todos los tests. */
    @Container
    @SuppressWarnings("resource")
    public static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("aulaia_test")
            .withUsername("test_user")
            .withPassword("test_pass")
            .withReuse(true); // Reutilizar entre tests para velocidad

    static {
        // Configurar propiedades de sistema para que Spring las use
        POSTGRES.start();
        System.setProperty("DB_HOST", POSTGRES.getHost());
        System.setProperty("DB_PORT", String.valueOf(POSTGRES.getFirstMappedPort()));
        System.setProperty("DB_NAME", POSTGRES.getDatabaseName());
        System.setProperty("DB_USERNAME", POSTGRES.getUsername());
        System.setProperty("DB_PASSWORD", POSTGRES.getPassword());
    }
}