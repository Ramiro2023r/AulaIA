package com.aulaia.service;

import com.aulaia.dto.justificacion.EvaluarJustificacionRequest;
import com.aulaia.dto.justificacion.JustificacionRequest;
import com.aulaia.dto.justificacion.JustificacionResponse;
import com.aulaia.entity.Asistencia;
import com.aulaia.entity.EstadoAsistencia;
import com.aulaia.entity.EstadoJustificacion;
import com.aulaia.entity.Justificacion;
import com.aulaia.entity.Usuario;
import com.aulaia.exception.BusinessException;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.mapper.JustificacionMapper;
import com.aulaia.repository.AsistenciaRepository;
import com.aulaia.repository.JustificacionRepository;
import com.aulaia.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JustificacionService {

    private final JustificacionRepository justificacionRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final JustificacionMapper justificacionMapper;
    private final AuditService auditService;

    @Transactional
    public JustificacionResponse crear(JustificacionRequest request, String solicitanteUsername) {
        Asistencia asistencia = asistenciaRepository.findById(request.asistenciaId())
                .orElseThrow(() -> new ResourceNotFoundException("Asistencia no encontrada"));

        if (justificacionRepository.findByAsistenciaId(asistencia.getId()).isPresent()) {
            throw new BusinessException("Ya existe una justificación asociada a esta asistencia");
        }

        Justificacion justificacion = new Justificacion();
        justificacion.setAsistencia(asistencia);
        justificacion.setMotivo(request.motivo());
        justificacion.setEstado(EstadoJustificacion.PENDIENTE);

        justificacion = justificacionRepository.save(justificacion);

        Usuario solicitante = usuarioRepository.findByUsername(solicitanteUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario solicitante no encontrado"));

        auditService.registrar(
                "justificacion",
                justificacion.getId(),
                "CREAR",
                null,
                Map.of("asistenciaId", asistencia.getId(), "motivo", justificacion.getMotivo())
        );

        return justificacionMapper.toResponse(justificacion);
    }

    @Transactional
    public JustificacionResponse evaluar(Long id, EvaluarJustificacionRequest request, String revisorUsername) {
        Justificacion justificacion = justificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Justificación no encontrada"));

        if (justificacion.getEstado() != EstadoJustificacion.PENDIENTE) {
            throw new BusinessException("La justificación ya ha sido evaluada");
        }

        Usuario revisor = usuarioRepository.findByUsername(revisorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario revisor no encontrado"));

        EstadoJustificacion estadoAnterior = justificacion.getEstado();
        justificacion.setEstado(request.estado());
        justificacion.setRevisadoPor(revisor);
        justificacion.setFechaRevision(OffsetDateTime.now());

        if (request.estado() == EstadoJustificacion.APROBADA) {
            Asistencia asistencia = justificacion.getAsistencia();
            String obsAnterior = asistencia.getObservacion();
            asistencia.setEstado(EstadoAsistencia.JUSTIFICADO);
            asistencia.setObservacion("Justificado: " + justificacion.getMotivo());
            asistenciaRepository.save(asistencia);
            
            auditService.registrar(
                    "asistencia",
                    asistencia.getId(),
                    "JUSTIFICAR",
                    Map.of("estado", EstadoAsistencia.AUSENTE, "observacion", obsAnterior != null ? obsAnterior : ""),
                    Map.of("estado", EstadoAsistencia.JUSTIFICADO, "observacion", asistencia.getObservacion())
            );
        }

        justificacion = justificacionRepository.save(justificacion);

        auditService.registrar(
                "justificacion",
                justificacion.getId(),
                "EVALUAR",
                Map.of("estado", estadoAnterior),
                Map.of("estado", justificacion.getEstado(), "revisadoPorId", revisor.getId())
        );

        return justificacionMapper.toResponse(justificacion);
    }

    @Transactional(readOnly = true)
    public List<JustificacionResponse> listarTodas() {
        return justificacionMapper.toResponseList(justificacionRepository.findAll());
    }

    @Transactional(readOnly = true)
    public JustificacionResponse obtenerPorId(Long id) {
        return justificacionRepository.findById(id)
                .map(justificacionMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Justificación no encontrada"));
    }
}
