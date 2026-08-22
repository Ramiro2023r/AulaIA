package com.aulaia.service;

import com.aulaia.dto.dashboard.AsistenciaRecienteDto;
import com.aulaia.dto.dashboard.DashboardDocenteResponse;
import com.aulaia.dto.dashboard.EstadisticasAsistencia;
import com.aulaia.dto.dashboard.EstudianteRiesgoDto;
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
import org.springframework.data.domain.PageRequest;
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
    private final SesionClaseService sesionClaseService;

    public DashboardDocenteService(
            HorarioRepository horarioRepository,
            SesionClaseRepository sesionClaseRepository,
            AsistenciaRepository asistenciaRepository,
            EstudianteRepository estudianteRepository,
            SesionClaseMapper sesionClaseMapper,
            UsuarioRepository usuarioRepository,
            DocenteRepository docenteRepository,
            SesionClaseService sesionClaseService) {
        this.horarioRepository = horarioRepository;
        this.sesionClaseRepository = sesionClaseRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.estudianteRepository = estudianteRepository;
        this.sesionClaseMapper = sesionClaseMapper;
        this.usuarioRepository = usuarioRepository;
        this.docenteRepository = docenteRepository;
        this.sesionClaseService = sesionClaseService;
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
        SesionClaseResponse primeraProgramada = null;

        for (Horario horario : horariosDelDia) {
            // Obtenemos o creamos la sesión para hoy (asegura que tenga ID)
            SesionClase sesion = sesionClaseService.obtenerOCrearSesion(horario.getId(), hoy);
            
            SesionClaseResponse responseDTO = sesionClaseMapper.toResponse(sesion);
            
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
            
            // Guardamos la primera programada como fallback
            if (sesion.getEstado() == SesionClaseEstado.PROGRAMADA && primeraProgramada == null) {
                primeraProgramada = responseDTO;
            }
            
            clasesDelDia.add(responseDTO);
            
            // Si no hemos determinado la clase actual (por sesión abierta), calculamos por horario
            if (claseActual == null) {
                if (!ahora.isBefore(horario.getHoraInicio()) && ahora.isBefore(horario.getHoraFin())) {
                    claseActual = responseDTO;
                }
            }
        }
        
        // Si no hay clase activa ni en el rango de hora, asignamos la primera programada del día
        if (claseActual == null && primeraProgramada != null) {
            claseActual = primeraProgramada;
        }
        
        double porcentaje = 0.0;
        if (totalEstudiantes > 0) {
            porcentaje = ((double) (presentes + tardanzas) / totalEstudiantes) * 100.0;
        }

        // Obtener Asistentes Actuales para la clase activa o programada más cercana
        Integer claseActualAsistentes = null;
        Integer claseActualTotalEstudiantes = null;
        if (claseActual != null) {
            // Ya calculamos totalEstudiantes de todas las clases del día, pero aquí queremos solo la de la clase actual
            Horario hActual = horarioRepository.findById(claseActual.horarioId()).orElse(null);
            if (hActual != null) {
                claseActualTotalEstudiantes = (int) estudianteRepository.countBySeccionIdAndActivoTrue(hActual.getSeccion().getId());
                List<Asistencia> asistenciasActual = asistenciaRepository.findBySesionClaseId(claseActual.id());
                claseActualAsistentes = (int) asistenciasActual.stream()
                        .filter(a -> a.getEstado() == EstadoAsistencia.PRESENTE || a.getEstado() == EstadoAsistencia.TARDANZA)
                        .count();
            }
        }

        EstadisticasAsistencia stats = new EstadisticasAsistencia(
                presentes, tardanzas, ausentes, totalEstudiantes, Math.round(porcentaje * 100.0) / 100.0
        );

        // Fetch recent attendances (last 5)
        List<Asistencia> recientes = asistenciaRepository.findUltimasAsistenciasPorDocente(docenteId, PageRequest.of(0, 5));
        List<AsistenciaRecienteDto> ultimosRegistros = recientes.stream().map(a -> new AsistenciaRecienteDto(
                a.getEstudiante().getNombres() + " " + a.getEstudiante().getApellidos(),
                a.getSesionClase().getHorario().getCurso().getNombre(),
                a.getEstado().name(),
                a.getFechaHora()
        )).toList();

        // Fetch students at risk (top 5)
        List<Object[]> riesgoRaw = asistenciaRepository.findEstudiantesEnRiesgo(docenteId, PageRequest.of(0, 5));
        List<EstudianteRiesgoDto> estudiantesRiesgo = riesgoRaw.stream().map(obj -> new EstudianteRiesgoDto(
                (Long) obj[0], // id
                obj[1] + " " + obj[2], // nombres + apellidos
                (String) obj[3], // curso
                (String) obj[4], // seccion
                ((Number) obj[5]).intValue(), // faltas
                0.0 // MVP: porcentaje no calculado estrictamente por ahora
        )).toList();

        return new DashboardDocenteResponse(claseActual, claseActualAsistentes, claseActualTotalEstudiantes, clasesDelDia, stats, estudiantesRiesgo, ultimosRegistros);
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
