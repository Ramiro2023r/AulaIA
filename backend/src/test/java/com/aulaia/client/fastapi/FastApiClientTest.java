package com.aulaia.client.fastapi;

import com.aulaia.client.fastapi.dto.FastApiAnalisisRequest;
import com.aulaia.client.fastapi.dto.FastApiAnalisisResponse;
import com.aulaia.client.fastapi.dto.FastApiAsistenciaEstudianteRequest;
import com.aulaia.client.fastapi.dto.FastApiHealthResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests básicos de configuración del cliente FastAPI.
 * <p>
 * Los tests de integración con mock de RestClient son complejos
 * por la API fluida genérica. Se validan en tests de integración
 * con Testcontainers o WireMock en sprints posteriores.
 */
@ExtendWith(MockitoExtension.class)
class FastApiClientTest {

    @Test
    void properties_defaultValues_areSet() {
        FastApiProperties props = new FastApiProperties();
        props.setBaseUrl("http://localhost:8000");

        assertEquals("http://localhost:8000", props.getBaseUrl());
        assertEquals(java.time.Duration.ofSeconds(5), props.getConnectTimeout());
        assertEquals(java.time.Duration.ofSeconds(30), props.getReadTimeout());
    }

    @Test
    void properties_customValues_areUsed() {
        FastApiProperties props = new FastApiProperties();
        props.setBaseUrl("http://fastapi:8000");
        props.setConnectTimeout(java.time.Duration.ofSeconds(10));
        props.setReadTimeout(java.time.Duration.ofSeconds(60));

        assertEquals("http://fastapi:8000", props.getBaseUrl());
        assertEquals(java.time.Duration.ofSeconds(10), props.getConnectTimeout());
        assertEquals(java.time.Duration.ofSeconds(60), props.getReadTimeout());
    }

    @Test
    void dto_analisisRequest_serializesCorrectly() {
        FastApiAnalisisRequest request = FastApiAnalisisRequest.builder()
                .estudiantes(List.of(
                        FastApiAsistenciaEstudianteRequest.builder()
                                .estudianteId(1L)
                                .nombre("Juan Pérez")
                                .presentes(8)
                                .tardanzas(2)
                                .ausentes(0)
                                .justificados(0)
                                .totalSesiones(10)
                                .build()
                ))
                .periodo("2026-08")
                .build();

        assertEquals(1, request.getEstudiantes().size());
        assertEquals("Juan Pérez", request.getEstudiantes().get(0).getNombre());
        assertEquals(8, request.getEstudiantes().get(0).getPresentes());
        assertEquals("2026-08", request.getPeriodo());
    }

    @Test
    void dto_healthResponse_hasRequiredFields() {
        FastApiHealthResponse response = FastApiHealthResponse.builder()
                .status("ok")
                .service("aulaia-fastapi")
                .module("analysis")
                .version("IA-1")
                .build();

        assertEquals("ok", response.getStatus());
        assertEquals("aulaia-fastapi", response.getService());
        assertEquals("analysis", response.getModule());
    }

    @Test
    void dto_analisisResponse_hasNestedObjects() {
        FastApiAnalisisResponse response = FastApiAnalisisResponse.builder()
                .resumenGeneral(FastApiAnalisisResponse.ResumenGeneral.builder()
                        .totalEstudiantes(30)
                        .porcentajeAsistenciaGlobal(92.5)
                        .tendenciaGlobal("ASCENDENTE")
                        .build())
                .insightsEstudiantes(List.of(
                        FastApiAnalisisResponse.InsightEstudiante.builder()
                                .estudianteId(1)
                                .nombre("Test")
                                .porcentajeAsistencia(100.0)
                                .tendencia("ASCENDENTE")
                                .nivelAtencion("ALTO")
                                .build()
                ))
                .patronesDetectados(List.of(
                        FastApiAnalisisResponse.PatronDetectado.builder()
                                .tipo("ASISTENCIA_PERFECTA")
                                .descripcion("1 estudiante con asistencia perfecta")
                                .severidad("info")
                                .build()
                ))
                .recomendaciones(List.of("Continuar monitoreo"))
                .build();

        assertEquals(30, response.getResumenGeneral().getTotalEstudiantes());
        assertEquals(92.5, response.getResumenGeneral().getPorcentajeAsistenciaGlobal());
        assertEquals(1, response.getInsightsEstudiantes().size());
        assertEquals(1, response.getPatronesDetectados().size());
        assertEquals(1, response.getRecomendaciones().size());
    }

    @Test
    void client_canBeInstantiated() {
        FastApiProperties props = new FastApiProperties();
        props.setBaseUrl("http://test:8000");

        RestClient restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .build();

        FastApiClient client = new FastApiClient(restClient, props);

        assertNotNull(client);
    }

    @Test
    void isAvailable_returnsFalseWhenHealthFails() {
        FastApiProperties props = new FastApiProperties();
        props.setBaseUrl("http://invalid:8000");
        RestClient restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .build();

        FastApiClient client = new FastApiClient(restClient, props);

        // Sin servidor real, health check falla y devuelve false
        boolean available = client.isAvailable();
        assertFalse(available);
    }
}