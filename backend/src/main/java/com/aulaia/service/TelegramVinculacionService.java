package com.aulaia.service;

import com.aulaia.dto.TelegramVinculacionResponseDto;
import com.aulaia.entity.Apoderado;
import com.aulaia.entity.EstadoVinculacion;
import com.aulaia.entity.Estudiante;
import com.aulaia.entity.EstudianteApoderado;
import com.aulaia.entity.TelegramVinculacion;
import com.aulaia.repository.ApoderadoRepository;
import com.aulaia.repository.EstudianteApoderadoRepository;
import com.aulaia.repository.EstudianteRepository;
import com.aulaia.repository.TelegramVinculacionRepository;
import com.aulaia.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "telegram", name = "enabled", havingValue = "true")
public class TelegramVinculacionService {

    private final TelegramVinculacionRepository vinculacionRepository;
    private final EstudianteRepository estudianteRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final EstudianteApoderadoRepository estudianteApoderadoRepository;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;
    
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public TelegramVinculacionResponseDto crearVinculacion(Long estudianteId, Long apoderadoId) {
        if (apoderadoId == null) {
            throw new BusinessException(
                    "Debe seleccionar un apoderado para vincular Telegram",
                    "TELEGRAM_APODERADO_REQUIRED");
        }

        // 1. Validar estudiante
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado: " + estudianteId));

        // 2 y 3. Validar apoderado activo y su relación con el estudiante.
        Apoderado apoderado = apoderadoRepository.findById(apoderadoId)
                .orElseThrow(() -> new IllegalArgumentException("Apoderado no encontrado: " + apoderadoId));
        if (!apoderado.isActivo()) {
            throw new BusinessException(
                    "El apoderado seleccionado está inactivo",
                    "TELEGRAM_APODERADO_INACTIVE");
        }

        boolean relacionados = estudianteApoderadoRepository.findByEstudianteId(estudianteId).stream()
                .anyMatch(rel -> rel.getApoderado().getId().equals(apoderadoId));

        if (!relacionados) {
            throw new IllegalArgumentException("El apoderado no está relacionado con el estudiante");
        }

        // 5. Revocar vinculaciones pendientes anteriores para este estudiante
        List<TelegramVinculacion> pendientes = vinculacionRepository.findByEstudianteIdAndEstado(estudianteId, EstadoVinculacion.PENDIENTE);
        for (TelegramVinculacion p : pendientes) {
            p.setEstado(EstadoVinculacion.REVOCADO);
            vinculacionRepository.save(p);
        }

        // 4. Generar token aleatorio criptográficamente seguro
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // 6. Guardar nueva vinculación (expira en 24h)
        TelegramVinculacion nuevaVinculacion = new TelegramVinculacion();
        nuevaVinculacion.setEstudiante(estudiante);
        nuevaVinculacion.setApoderado(apoderado);
        nuevaVinculacion.setToken(token);
        nuevaVinculacion.setEstado(EstadoVinculacion.PENDIENTE);
        nuevaVinculacion.setExpiresAt(LocalDateTime.now(clock).plusHours(24));
        
        vinculacionRepository.save(nuevaVinculacion);

        // 7. Devolver DTO
        return TelegramVinculacionResponseDto.builder()
                .token(nuevaVinculacion.getToken())
                .estado(nuevaVinculacion.getEstado())
                .expiresAt(nuevaVinculacion.getExpiresAt())
                .build();
    }

    @Transactional
    public void procesarComandoStart(String token, Long chatId) {
        if (token == null || token.trim().isEmpty() || chatId == null) {
            return;
        }

        // El bloqueo pesimista conserva la semántica de token de un solo uso
        // incluso si el polling llegase a ejecutarse en más de una instancia.
        TelegramVinculacion vinculacion = vinculacionRepository.findByTokenForUpdate(token)
                .orElse(null);

        if (vinculacion == null) {
            return; // Token inexistente
        }

        if (vinculacion.getEstado() != EstadoVinculacion.PENDIENTE) {
            return; // Token expirado, revocado o ya vinculado
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        if (!vinculacion.getExpiresAt().isAfter(ahora)) {
            vinculacion.setEstado(EstadoVinculacion.EXPIRADO);
            vinculacionRepository.save(vinculacion);
            return; // Token expirado
        }

        // Vincular
        vinculacion.setEstado(EstadoVinculacion.VINCULADO);
        vinculacion.setUsedAt(ahora);

        if (vinculacion.getApoderado() != null) {
            Apoderado apoderado = vinculacion.getApoderado();
            apoderado.setTelegramChatId(String.valueOf(chatId));
            apoderado.setTelegramVinculadoAt(java.time.OffsetDateTime.now(clock));
            apoderadoRepository.save(apoderado);
        }

        vinculacionRepository.save(vinculacion);

        // El listener se ejecuta AFTER_COMMIT: un fallo de Telegram no puede
        // revertir la vinculación ya confirmada en la base de datos.
        eventPublisher.publishEvent(new TelegramVinculacionConfirmadaEvent(
                chatId, nombreSeguroEstudiante(vinculacion.getEstudiante())));
    }

    private String nombreSeguroEstudiante(Estudiante estudiante) {
        if (estudiante == null) {
            return null;
        }
        String nombres = estudiante.getNombres() == null ? "" : estudiante.getNombres().trim();
        String apellidos = estudiante.getApellidos() == null ? "" : estudiante.getApellidos().trim();
        String nombreCompleto = (nombres + " " + apellidos).trim();
        return nombreCompleto.isBlank() ? null : nombreCompleto;
    }
}
