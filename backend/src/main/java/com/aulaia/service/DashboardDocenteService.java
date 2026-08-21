package com.aulaia.service;

import com.aulaia.dto.dashboard.DashboardDocenteResponse;
import com.aulaia.dto.dashboard.EstadisticasAsistencia;
import com.aulaia.dto.sesion.SesionClaseResponse;
import com.aulaia.entity.Asistencia;
import com.aulaia.entity.EstadoAsistencia;
import com.aulaia.entity.Horario;
import com.aulaia.entity.SesionClase;
import com.aulaia.entity.SesionClaseEstado;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.mapper.SesionClaseMapper;
import com.aulaia.repository.AsistenciaRepository;
import com.aulaia.repository.DocenteRepository;
import com.aulaia.repository.EstudianteRepository;
import com.aulaia.repository.HorarioRepository;
import com.aulaia.repository.SesionClaseRepository;
import com.aulaia.repository.UsuarioRepository;
import com.aulaia.entity.Docente;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class DashboardDocenteService {

    private final HorarioRepository horarioRepository;
    private final SesionClaseRepository sesionClaseRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final EstudianteRepository estudianteRepository;
    private final SesionClaseMapper sesionClaseMapper;
    private final UsuarioRepository usuarioRepository;
    private final DocenteRepository docenteRepository;

    public DashboardDocenteService(
            HorarioRepository horarioRepository,
            SesionClaseRepository sesionClaseRepository,
            AsistenciaRepository asistenciaRepository,
            EstudianteRepository estudianteRepository,
            SesionClaseMapper sesionClaseMapper,
            UsuarioRepository usuarioRepository,
            DocenteRepository docenteRepository) {
        this.horarioRepository = horarioRepository;
        this.sesionClaseRepository = sesionClaseRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.estudianteRepository = estudianteRepository;
        this.sesionClaseMapper = sesionClaseMapper;
        this.usuarioRepository = usuarioRepository;
        this.docenteRepository = docenteRepository;
    }

    public DashboardDocenteResponse obtenerResumen() {
        Long docenteId = obtenerIdDocenteAutenticado();
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        // El día de la semana en Java (Monday=1, Sunday=7) mapea directo al nuestro.
        Short diaSemana = (short) hoy.getDayOfWeek().getValue();

        List<Horario> horariosDelDia = horarioRepository.buscarConFiltros(docenteId, null, null, diaSemana);
        
        List<SesionClaseResponse> clasesDelDia = new ArrayList<>();
        SesionClaseResponse claseActual = null;
        
        int presentes = 0;
        int tardanzas = 0;
        int ausentes = 0;
        int totalEstudiantes = 0;

        for (Horario horario : horariosDelDia) {
            // Buscamos si ya existe la sesión para hoy
            Optional<SesionClase> sesionOpt = sesionClaseRepository.findByHorarioIdAndFecha(horario.getId(), hoy);
            
            SesionClaseResponse responseDTO;
            
            if (sesionOpt.isPresent()) {
                SesionClase sesion = sesionOpt.get();
                responseDTO = sesionClaseMapper.toResponse(sesion);
                
                // Contar métricas
                totalEstudiantes += estudianteRepository.countBySeccionIdAndActivoTrue(horario.getSeccion().getId());
                
                List<Asistencia> asistencias = asistenciaRepository.findBySesionClaseId(sesion.getId());
                for (Asistencia a : asistencias) {
                    if (a.getEstado() == EstadoAsistencia.PRESENTE) presentes++;
                    else if (a.getEstado() == EstadoAsistencia.TARDANZA) tardanzas++;
                    else if (a.getEstado() == EstadoAsistencia.AUSENTE) ausentes++;
                }
                
                // Si la sesión está ABIERTA, es automáticamente la clase actual
                if (sesion.getEstado() == SesionClaseEstado.ABIERTA) {
                    claseActual = responseDTO;
                }
            } else {
                // Sesión no creada aún. Enviamos DTO simulado para la UI.
                responseDTO = new SesionClaseResponse(
                        null,
                        horario.getId(),
                        hoy,
                        null, // Sin estado porque no existe
                        null,
                        null,
                        horario.getHoraInicio(),
                        horario.getHoraFin(),
                        new SesionClaseResponse.CursoResumen(horario.getCurso().getId(), horario.getCurso().getNombre()),
                        new SesionClaseResponse.SeccionResumen(horario.getSeccion().getId(), horario.getSeccion().getNombre()),
                        new SesionClaseResponse.DocenteResumen(horario.getDocente().getId(), horario.getDocente().getNombres(), horario.getDocente().getApellidos())
                );
            }
            
            clasesDelDia.add(responseDTO);
            
            // Si no hemos determinado la clase actual (por sesión abierta), calculamos por horario
            if (claseActual == null) {
                if (!ahora.isBefore(horario.getHoraInicio()) && ahora.isBefore(horario.getHoraFin())) {
                    claseActual = responseDTO;
                }
            }
        }
        
        double porcentaje = 0.0;
        if (totalEstudiantes > 0) {
            porcentaje = ((double) (presentes + tardanzas) / totalEstudiantes) * 100.0;
        }

        EstadisticasAsistencia stats = new EstadisticasAsistencia(
                presentes, tardanzas, ausentes, totalEstudiantes, Math.round(porcentaje * 100.0) / 100.0
        );

        return new DashboardDocenteResponse(claseActual, clasesDelDia, stats);
    }

    private Long obtenerIdDocenteAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails principal)) {
            throw new RuntimeException("Autenticación requerida");
        }
        return usuarioRepository.findByUsername(principal.getUsername())
                .flatMap(usuario -> docenteRepository.findByUsuarioId(usuario.getId()))
                .map(Docente::getId)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no tiene un perfil docente asociado", "TEACHER_NOT_FOUND"));
    }
}
