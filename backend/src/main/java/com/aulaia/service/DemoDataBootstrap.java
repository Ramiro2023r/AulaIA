package com.aulaia.service;

import com.aulaia.entity.*;
import com.aulaia.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Component
@Profile("dev")
public class DemoDataBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataBootstrap.class);

    private final UsuarioRepository usuarioRepository;
    private final DocenteRepository docenteRepository;
    private final CursoRepository cursoRepository;
    private final GradoRepository gradoRepository;
    private final SeccionRepository seccionRepository;
    private final EstudianteRepository estudianteRepository;
    private final HorarioRepository horarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataBootstrap(UsuarioRepository usuarioRepository, DocenteRepository docenteRepository, CursoRepository cursoRepository, GradoRepository gradoRepository, SeccionRepository seccionRepository, EstudianteRepository estudianteRepository, HorarioRepository horarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.docenteRepository = docenteRepository;
        this.cursoRepository = cursoRepository;
        this.gradoRepository = gradoRepository;
        this.seccionRepository = seccionRepository;
        this.estudianteRepository = estudianteRepository;
        this.horarioRepository = horarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Iniciando carga de datos de demostración para perfil dev...");

        if (usuarioRepository.existsByUsername("docente@aulaia.com")) {
            log.info("Usuario docente de prueba ya existe. Omitiendo bootstrap.");
            return;
        }

        Usuario docenteUsuario = new Usuario();
        docenteUsuario.setUsername("docente@aulaia.com");
        docenteUsuario.setPasswordHash(passwordEncoder.encode("123456"));
        docenteUsuario.setRol(Rol.DOCENTE);
        docenteUsuario.setActivo(true);
        docenteUsuario = usuarioRepository.save(docenteUsuario);

        Docente docente = new Docente();
        docente.setUsuario(docenteUsuario);
        docente.setNombres("Juan");
        docente.setApellidos("Pérez");
        docente.setActivo(true);
        docente = docenteRepository.save(docente);

        Grado grado = new Grado();
        grado.setNombre("6.º Primaria");
        grado.setNivel("PRIMARIA");
        grado = gradoRepository.save(grado);

        Seccion seccion = new Seccion();
        seccion.setGrado(grado);
        seccion.setNombre("A");
        seccion.setPeriodoAcademico("2026");
        seccion.setActivo(true);
        seccion = seccionRepository.save(seccion);

        Curso curso = new Curso();
        curso.setNombre("Historia");
        curso.setActivo(true);
        curso = cursoRepository.save(curso);

        Estudiante e1 = new Estudiante();
        e1.setSeccion(seccion);
        e1.setCodigo("EST-001");
        e1.setQrToken("qr-token-est-001-" + System.currentTimeMillis());
        e1.setNombres("Carlos");
        e1.setApellidos("Gómez");
        e1.setActivo(true);
        estudianteRepository.save(e1);

        Estudiante e2 = new Estudiante();
        e2.setSeccion(seccion);
        e2.setCodigo("EST-002");
        e2.setQrToken("qr-token-est-002-" + System.currentTimeMillis());
        e2.setNombres("María");
        e2.setApellidos("López");
        e2.setActivo(true);
        estudianteRepository.save(e2);

        Estudiante e3 = new Estudiante();
        e3.setSeccion(seccion);
        e3.setCodigo("EST-003");
        e3.setQrToken("qr-token-est-003-" + System.currentTimeMillis());
        e3.setNombres("Luis");
        e3.setApellidos("Sánchez");
        e3.setActivo(true);
        estudianteRepository.save(e3);

        Horario horario = new Horario();
        horario.setDocente(docente);
        horario.setSeccion(seccion);
        horario.setCurso(curso);
        horario.setDiaSemana((short) 1); // Lunes
        horario.setHoraInicio(LocalTime.of(8, 0));
        horario.setHoraFin(LocalTime.of(10, 0));
        horario.setToleranciaMinutos((short) 15);
        horario.setActivo(true);
        horarioRepository.save(horario);

        Horario horario2 = new Horario();
        horario2.setDocente(docente);
        horario2.setSeccion(seccion);
        horario2.setCurso(curso);
        horario2.setDiaSemana((short) 2); // Martes
        horario2.setHoraInicio(LocalTime.of(8, 0));
        horario2.setHoraFin(LocalTime.of(10, 0));
        horario2.setToleranciaMinutos((short) 15);
        horario2.setActivo(true);
        horarioRepository.save(horario2);

        Horario horario3 = new Horario();
        horario3.setDocente(docente);
        horario3.setSeccion(seccion);
        horario3.setCurso(curso);
        horario3.setDiaSemana((short) 3); // Miercoles
        horario3.setHoraInicio(LocalTime.of(8, 0));
        horario3.setHoraFin(LocalTime.of(10, 0));
        horario3.setToleranciaMinutos((short) 15);
        horario3.setActivo(true);
        horarioRepository.save(horario3);

        Horario horario4 = new Horario();
        horario4.setDocente(docente);
        horario4.setSeccion(seccion);
        horario4.setCurso(curso);
        horario4.setDiaSemana((short) 4); // Jueves
        horario4.setHoraInicio(LocalTime.of(8, 0));
        horario4.setHoraFin(LocalTime.of(10, 0));
        horario4.setToleranciaMinutos((short) 15);
        horario4.setActivo(true);
        horarioRepository.save(horario4);
        
        Horario horario5 = new Horario();
        horario5.setDocente(docente);
        horario5.setSeccion(seccion);
        horario5.setCurso(curso);
        horario5.setDiaSemana((short) 5); // Viernes
        horario5.setHoraInicio(LocalTime.of(8, 0));
        horario5.setHoraFin(LocalTime.of(10, 0));
        horario5.setToleranciaMinutos((short) 15);
        horario5.setActivo(true);
        horarioRepository.save(horario5);


        log.info("Datos de demostración creados exitosamente. Puedes ingresar con docente@aulaia.com / 123456");
    }
}
