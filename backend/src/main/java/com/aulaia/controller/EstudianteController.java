package com.aulaia.controller;

import com.aulaia.dto.ApiErrorResponse;
import com.aulaia.dto.estudiante.EstudianteRequest;
import com.aulaia.dto.estudiante.EstudianteResponse;
import com.aulaia.dto.estudiante.RegenerarQrResponse;
import com.aulaia.dto.estudiante.ApoderadoEstudianteRequest;
import com.aulaia.dto.telegram.ApoderadoTelegramOptionResponse;
import com.aulaia.entity.EstudianteApoderado;
import com.aulaia.exception.BusinessException;
import com.aulaia.repository.EstudianteApoderadoRepository;
import com.aulaia.service.EstudianteService;
import com.aulaia.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de estudiantes (Prompt 4.3, 07-PLAN).
 *
 * <p>Autorización: GET para ADMIN y DOCENTE (las restricciones de contexto
 * por sección para DOCENTE se aplicarán en un prompt posterior); POST/PUT/
 * PATCH solo ADMIN ({@code @PreAuthorize}). Sin DELETE: el plan no lo
 * define; la desactivación es el mecanismo documentado (PATCH desactivar,
 * 06-FLUJOS #48). El qrToken no entra desde el frontend (lo genera el
 * Service) ni se expone en respuestas.
 */
@RestController
@RequestMapping("/api/v1/estudiantes")
@Tag(name = "Estudiantes", description = "Estudiantes y su sección (módulo Estudiantes, Sprint 4)")
public class EstudianteController {

    private final EstudianteService estudianteService;
    private final QrCodeService qrCodeService;
    private final java.util.Optional<com.aulaia.service.TelegramVinculacionService> telegramVinculacionService;
    private final com.aulaia.config.TelegramProperties telegramProperties;
    private final ObjectProvider<EstudianteApoderadoRepository> estudianteApoderadoRepository;
    private final com.aulaia.service.EstudianteApoderadoService estudianteApoderadoService;

    public EstudianteController(EstudianteService estudianteService, QrCodeService qrCodeService,
                                java.util.Optional<com.aulaia.service.TelegramVinculacionService> telegramVinculacionService,
                                com.aulaia.config.TelegramProperties telegramProperties,
                                ObjectProvider<EstudianteApoderadoRepository> estudianteApoderadoRepository,
                                com.aulaia.service.EstudianteApoderadoService estudianteApoderadoService) {
        this.estudianteService = estudianteService;
        this.qrCodeService = qrCodeService;
        this.telegramVinculacionService = telegramVinculacionService;
        this.telegramProperties = telegramProperties;
        this.estudianteApoderadoRepository = estudianteApoderadoRepository;
        this.estudianteApoderadoService = estudianteApoderadoService;
    }

    @GetMapping
    @Operation(summary = "Listar estudiantes",
            description = "ADMIN y DOCENTE. Filtros combinables: codigo (igualdad exacta), "
                    + "nombre (coincidencia parcial sobre nombres), seccion (seccionId) y activo. "
                    + "Orden estable por id. Sin paginación.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de estudiantes",
                    content = @Content(schema = @Schema(implementation = EstudianteResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<EstudianteResponse>> listar(
            @Parameter(description = "Filtro por codigo (igualdad exacta)")
            @RequestParam(required = false) String codigo,
            @Parameter(description = "Filtro por nombre (coincidencia parcial sobre nombres)")
            @RequestParam(required = false) String nombre,
            @Parameter(description = "Búsqueda general (parcial, case-insensitive) sobre codigo, nombres y apellidos")
            @RequestParam(required = false) String buscar,
            @Parameter(description = "Filtro por id de seccion")
            @RequestParam(required = false) Long seccion,
            @Parameter(description = "Filtro por estado activo")
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(estudianteService.listar(codigo, nombre, buscar, seccion, activo));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener estudiante por id",
            description = "ADMIN y DOCENTE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estudiante encontrado",
                    content = @Content(schema = @Schema(implementation = EstudianteResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "STUDENT_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<EstudianteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(estudianteService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear estudiante",
            description = "Solo ADMIN. El qrToken lo genera el Service de forma segura; "
                    + "404 SECTION_NOT_FOUND si la sección no existe; 409 STUDENT_CODE_ALREADY_EXISTS "
                    + "si el codigo ya está en uso.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Estudiante creado",
                    content = @Content(schema = @Schema(implementation = EstudianteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (Bean Validation / JSON malformado)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede crear estudiantes",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "SECTION_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "STUDENT_CODE_ALREADY_EXISTS",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<EstudianteResponse> crear(@Valid @RequestBody EstudianteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estudianteService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar estudiante",
            description = "Solo ADMIN. No regenera el qrToken. "
                    + "404 STUDENT_NOT_FOUND si el estudiante no existe; 404 SECTION_NOT_FOUND si la "
                    + "sección no existe; 409 STUDENT_CODE_ALREADY_EXISTS si el codigo ya pertenece a "
                    + "otro estudiante.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estudiante actualizado",
                    content = @Content(schema = @Schema(implementation = EstudianteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (Bean Validation / JSON malformado)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede modificar estudiantes",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "STUDENT_NOT_FOUND / SECTION_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "STUDENT_CODE_ALREADY_EXISTS",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<EstudianteResponse> actualizar(@PathVariable Long id,
                                                         @Valid @RequestBody EstudianteRequest request) {
        return ResponseEntity.ok(estudianteService.actualizar(id, request));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar estudiante",
            description = "Solo ADMIN. activo = false sin borrado físico (06-FLUJOS #48); "
                    + "conserva codigo, qrToken, nombres, apellidos y sección. Idempotente: "
                    + "desactivar un estudiante ya inactivo no produce error.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estudiante desactivado",
                    content = @Content(schema = @Schema(implementation = EstudianteResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede desactivar estudiantes",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "STUDENT_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<EstudianteResponse> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(estudianteService.desactivar(id));
    }

    @PostMapping("/{id}/regenerar-qr")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Regenerar token QR",
            description = "Solo ADMIN (Prompt 4.4, 07-PLAN). Genera un nuevo qrToken opaco y "
                    + "único; el anterior queda inválido al dejar de persistirse. El token no se "
                    + "expone (privacidad: 04-BD §22); el refresco visual del QR corresponde al "
                    + "endpoint de imagen (Prompt 4.5). No se genera imagen QR aquí.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "QR regenerado",
                    content = @Content(schema = @Schema(implementation = RegenerarQrResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede regenerar el QR",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "STUDENT_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<RegenerarQrResponse> regenerarQr(@PathVariable Long id) {
        return ResponseEntity.ok(estudianteService.regenerarQrToken(id));
    }

    @GetMapping("/{id}/qr")
    @Operation(summary = "Obtener imagen QR del estudiante",
            description = "ADMIN y DOCENTE (02-TRD: obtener QR autorizado). Genera bajo demanda el "
                    + "PNG del QR con contenido exacto AULAIA:STUDENT:<qrToken> (07-PLAN 4.5, "
                    + "06-FLUJOS #7); sin datos personales (04-BD §22), sin regenerar el token. "
                    + "La imagen no se persiste ni se cachea.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Imagen QR en PNG",
                    content = @Content(mediaType = "image/png")),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "STUDENT_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<byte[]> obtenerQr(@PathVariable Long id) {
        String contenido = estudianteService.contenidoQr(id);
        byte[] png = qrCodeService.generarPng(contenido);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    @GetMapping("/{estudianteId}/apoderados")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCENTE')")
    @Operation(summary = "Listar apoderados para vinculación Telegram",
            description = "Devuelve los apoderados relacionados con el estudiante para seleccionar el destinatario de Telegram.")
    public ResponseEntity<List<ApoderadoTelegramOptionResponse>> listarApoderadosParaTelegram(
            @PathVariable Long estudianteId) {
        estudianteService.buscarPorId(estudianteId);
        List<ApoderadoTelegramOptionResponse> apoderados = estudianteApoderadoRepository.getObject()
                .findWithApoderadoByEstudianteId(estudianteId)
                .stream()
                .map(this::toApoderadoTelegramOption)
                .toList();
        return ResponseEntity.ok(apoderados);
    }

    @PostMapping("/{estudianteId}/apoderados")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar y asociar apoderado",
            description = "Crea un apoderado y lo asocia al estudiante. Si se marca como principal, reemplaza al principal previo de ese estudiante.")
    public ResponseEntity<ApoderadoTelegramOptionResponse> crearApoderado(
            @PathVariable Long estudianteId,
            @Valid @RequestBody ApoderadoEstudianteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estudianteApoderadoService.crearYAsociar(estudianteId, request));
    }

    @PostMapping("/{estudianteId}/telegram/vinculacion")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCENTE')")
    @Operation(summary = "Generar invitación para Telegram",
            description = "ADMIN y DOCENTE. Crea una vinculación temporal para un estudiante y "
                    + "opcionalmente su apoderado. Retorna el deep link de Telegram si el bot está habilitado. "
                    + "Falla con 409 o 400 si Telegram no está habilitado o configurado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vinculación generada exitosamente",
                    content = @Content(schema = @Schema(implementation = com.aulaia.dto.telegram.TelegramVinculacionLinkResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos / Bot no configurado / Apoderado no relacionado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Telegram deshabilitado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Estudiante o Apoderado no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<com.aulaia.dto.telegram.TelegramVinculacionLinkResponse> generarVinculacionTelegram(
            @PathVariable Long estudianteId,
            @RequestBody(required = false) com.aulaia.dto.telegram.TelegramVinculacionRequest request) {

        if (!telegramProperties.isEnabled() || telegramVinculacionService.isEmpty()) {
            throw new com.aulaia.exception.ConflictException("La integración con Telegram está deshabilitada en la configuración", "TELEGRAM_DISABLED");
        }

        String botUsername = telegramProperties.getBot().getUsername();
        if (botUsername == null || botUsername.trim().isEmpty()) {
            throw new com.aulaia.exception.BusinessException("El nombre de usuario del bot de Telegram no está configurado", "TELEGRAM_NOT_CONFIGURED");
        }

        // Validate estudiante
        estudianteService.buscarPorId(estudianteId);

        Long apoderadoId = (request != null) ? request.getApoderadoId() : null;
        if (apoderadoId == null) {
            throw new BusinessException(
                    "Debe seleccionar un apoderado para vincular Telegram",
                    "TELEGRAM_APODERADO_REQUIRED");
        }

        try {
            com.aulaia.dto.TelegramVinculacionResponseDto vinculacion = 
                    telegramVinculacionService.get().crearVinculacion(estudianteId, apoderadoId);

            String telegramUrl = "https://t.me/" + botUsername + "?start=" + vinculacion.getToken();

            com.aulaia.dto.telegram.TelegramVinculacionLinkResponse response = 
                    com.aulaia.dto.telegram.TelegramVinculacionLinkResponse.builder()
                            .status(vinculacion.getEstado().name())
                            .telegramUrl(telegramUrl)
                            .expiresAt(vinculacion.getExpiresAt().toString())
                            .build();

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Either apoderado not found or not related
            throw new com.aulaia.exception.BusinessException(e.getMessage(), "INVALID_RELATION");
        }
    }

    private ApoderadoTelegramOptionResponse toApoderadoTelegramOption(EstudianteApoderado relacion) {
        return new ApoderadoTelegramOptionResponse(
                relacion.getApoderado().getId(),
                relacion.getApoderado().getNombres(),
                relacion.getApoderado().getApellidos(),
                relacion.getParentesco().name(),
                relacion.isPrincipal(),
                relacion.getApoderado().isActivo());
    }
}
