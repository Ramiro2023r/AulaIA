package com.aulaia.service;

import com.aulaia.dto.estudiante.ApoderadoEstudianteRequest;
import com.aulaia.dto.estudiante.ApoderadoDisponibleResponse;
import com.aulaia.dto.estudiante.AsociarApoderadoRequest;
import com.aulaia.dto.telegram.ApoderadoTelegramOptionResponse;
import com.aulaia.entity.Apoderado;
import com.aulaia.entity.Estudiante;
import com.aulaia.entity.EstudianteApoderado;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.repository.ApoderadoRepository;
import com.aulaia.repository.EstudianteApoderadoRepository;
import com.aulaia.repository.EstudianteRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestiona el alta de un apoderado en el contexto de un estudiante. */
@Service
public class EstudianteApoderadoService {

    private final ObjectProvider<EstudianteRepository> estudianteRepository;
    private final ObjectProvider<ApoderadoRepository> apoderadoRepository;
    private final ObjectProvider<EstudianteApoderadoRepository> estudianteApoderadoRepository;

    public EstudianteApoderadoService(ObjectProvider<EstudianteRepository> estudianteRepository,
                                      ObjectProvider<ApoderadoRepository> apoderadoRepository,
                                      ObjectProvider<EstudianteApoderadoRepository> estudianteApoderadoRepository) {
        this.estudianteRepository = estudianteRepository;
        this.apoderadoRepository = apoderadoRepository;
        this.estudianteApoderadoRepository = estudianteApoderadoRepository;
    }

    @Transactional
    public ApoderadoTelegramOptionResponse crearYAsociar(Long estudianteId, ApoderadoEstudianteRequest request) {
        Estudiante estudiante = estudianteRepository.getObject().findById(estudianteId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado", "STUDENT_NOT_FOUND"));

        Apoderado apoderado = new Apoderado();
        apoderado.setNombres(request.nombres().trim());
        apoderado.setApellidos(request.apellidos().trim());
        apoderado.setTelefono(normalizarTelefono(request.telefono()));
        apoderado.setActivo(true);
        Apoderado guardado = apoderadoRepository.getObject().save(apoderado);

        EstudianteApoderado relacion = new EstudianteApoderado();
        relacion.setEstudiante(estudiante);
        relacion.setApoderado(guardado);
        relacion.setParentesco(request.parentesco());
        relacion.setPrincipal(request.principal());

        if (request.principal()) {
            estudianteApoderadoRepository.getObject().findByEstudianteId(estudianteId)
                    .forEach(existente -> existente.setPrincipal(false));
        }

        estudianteApoderadoRepository.getObject().save(relacion);
        return new ApoderadoTelegramOptionResponse(
                guardado.getId(),
                guardado.getNombres(),
                guardado.getApellidos(),
                relacion.getParentesco().name(),
                relacion.isPrincipal(),
                guardado.isActivo(),
                tieneTelegramVinculado(guardado));
    }

    @Transactional(readOnly = true)
    public List<ApoderadoDisponibleResponse> buscarDisponibles(Long estudianteId, String buscar) {
        estudianteRepository.getObject().findById(estudianteId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado", "STUDENT_NOT_FOUND"));

        String termino = buscar == null || buscar.isBlank() ? null : buscar.trim();
        return apoderadoRepository.getObject()
                .buscarActivosNoAsociados(estudianteId, termino, PageRequest.of(0, 20))
                .stream()
                .map(apoderado -> new ApoderadoDisponibleResponse(
                        apoderado.getId(),
                        apoderado.getNombres(),
                        apoderado.getApellidos(),
                        apoderado.getTelefono()))
                .toList();
    }

    @Transactional
    public ApoderadoTelegramOptionResponse asociarExistente(Long estudianteId, Long apoderadoId,
                                                              AsociarApoderadoRequest request) {
        Estudiante estudiante = estudianteRepository.getObject().findById(estudianteId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado", "STUDENT_NOT_FOUND"));
        Apoderado apoderado = apoderadoRepository.getObject().findById(apoderadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Apoderado no encontrado", "PARENT_NOT_FOUND"));

        if (!apoderado.isActivo()) {
            throw new com.aulaia.exception.BusinessException(
                    "El apoderado seleccionado está inactivo", "PARENT_INACTIVE");
        }
        if (estudianteApoderadoRepository.getObject().existsByEstudianteIdAndApoderadoId(estudianteId, apoderadoId)) {
            throw new com.aulaia.exception.ConflictException(
                    "El apoderado ya está asociado a este estudiante", "PARENT_ALREADY_ASSOCIATED");
        }

        if (request.principal()) {
            estudianteApoderadoRepository.getObject().findByEstudianteId(estudianteId)
                    .forEach(existente -> existente.setPrincipal(false));
        }

        EstudianteApoderado relacion = new EstudianteApoderado();
        relacion.setEstudiante(estudiante);
        relacion.setApoderado(apoderado);
        relacion.setParentesco(request.parentesco());
        relacion.setPrincipal(request.principal());
        estudianteApoderadoRepository.getObject().save(relacion);

        return new ApoderadoTelegramOptionResponse(
                apoderado.getId(),
                apoderado.getNombres(),
                apoderado.getApellidos(),
                relacion.getParentesco().name(),
                relacion.isPrincipal(),
                apoderado.isActivo(),
                tieneTelegramVinculado(apoderado));
    }

    private boolean tieneTelegramVinculado(Apoderado apoderado) {
        return apoderado.getTelegramChatId() != null
                && !apoderado.getTelegramChatId().isBlank()
                && apoderado.getTelegramVinculadoAt() != null;
    }

    private String normalizarTelefono(String telefono) {
        return telefono == null || telefono.isBlank() ? null : telefono.trim();
    }
}
