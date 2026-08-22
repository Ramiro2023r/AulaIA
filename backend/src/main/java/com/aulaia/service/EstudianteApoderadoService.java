package com.aulaia.service;

import com.aulaia.dto.estudiante.ApoderadoEstudianteRequest;
import com.aulaia.dto.telegram.ApoderadoTelegramOptionResponse;
import com.aulaia.entity.Apoderado;
import com.aulaia.entity.Estudiante;
import com.aulaia.entity.EstudianteApoderado;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.repository.ApoderadoRepository;
import com.aulaia.repository.EstudianteApoderadoRepository;
import com.aulaia.repository.EstudianteRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                guardado.isActivo());
    }

    private String normalizarTelefono(String telefono) {
        return telefono == null || telefono.isBlank() ? null : telefono.trim();
    }
}
