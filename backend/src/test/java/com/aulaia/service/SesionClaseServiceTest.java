package com.aulaia.service;

import com.aulaia.entity.Curso;
import com.aulaia.dto.sesion.SesionClaseResponse;
import com.aulaia.entity.Docente;
import com.aulaia.entity.Horario;
import com.aulaia.entity.Rol;
import com.aulaia.entity.Seccion;
import com.aulaia.entity.SesionClase;
import com.aulaia.entity.SesionClaseEstado;
import com.aulaia.entity.Usuario;
import com.aulaia.entity.Estudiante;
import com.aulaia.exception.BusinessException;
import com.aulaia.exception.ConflictException;
import com.aulaia.exception.ForbiddenOperationException;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.mapper.SesionClaseMapperImpl;
import com.aulaia.repository.AsistenciaRepository;
import com.aulaia.repository.DocenteRepository;
import com.aulaia.repository.EstudianteRepository;
import com.aulaia.repository.HorarioRepository;
import com.aulaia.repository.SesionClaseRepository;
import com.aulaia.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link SesionClaseService} (Prompts 6.2 y 6.3,
 * 07-PLAN), con repositorios y creador mockeados. No dependen de
 * PostgreSQL; las restricciones físicas (UNIQUE, FK) se cubren contra BD
 * real en {@code SesionClaseRepositoryTest}/{@code
 * SesionClaseServiceRepositoryTest}. Datos ficticios, nunca reales.
 *
 * <p>Prompt 6.3: reloj inyectable fijo (2026-08-19T14:30:45-05:00) para
 * pruebas deterministas de {@code horaApertura}; la identidad del DOCENTE
 * se simula con el {@link SecurityContextHolder} (JWT → principal → Usuario
 * → Docente), nunca con ids del request.
 */
class SesionClaseServiceTest {

    private static final OffsetDateTime HORA_FIJA = OffsetDateTime.parse("2026-08-19T14:30:45-05:00");

    private final HorarioRepository horarioRepository = mock(HorarioRepository.class);
    private final SesionClaseRepository sesionClaseRepository = mock(SesionClaseRepository.class);
    private final SesionClaseCreatorTx sesionClaseCreatorTx = mock(SesionClaseCreatorTx.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final DocenteRepository docenteRepository = mock(DocenteRepository.class);
    private final AsistenciaRepository asistenciaRepository = mock(AsistenciaRepository.class);
    private final EstudianteRepository estudianteRepository = mock(EstudianteRepository.class);
    private final AuditService auditService = mock(AuditService.class);
    private final com.aulaia.mapper.SesionClaseMapper sesionClaseMapper = org.mapstruct.factory.Mappers.getMapper(com.aulaia.mapper.SesionClaseMapper.class);
    private final Clock clock = Clock.fixed(HORA_FIJA.toInstant(), ZoneId.of("America/Lima"));
    private final SesionClaseService sesionClaseService = new SesionClaseService(
            horarioRepository, sesionClaseRepository, sesionClaseCreatorTx,
            usuarioRepository, docenteRepository, asistenciaRepository, estudianteRepository, sesionClaseMapper, auditService, clock);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Horario horario(Long id) {
        return horario(id, null);
    }

    private Horario horario(Long id, Docente docente) {
        Horario horario = new Horario();
        horario.setId(id);
        horario.setCurso(new Curso());
        horario.setSeccion(new Seccion());
        horario.setDocente(docente != null ? docente : new Docente());
        horario.setDiaSemana((short) 1);
        horario.setHoraInicio(LocalTime.of(9, 0));
        horario.setHoraFin(LocalTime.of(10, 30));
        return horario;
    }

    private Docente docente(Long id) {
        Docente docente = new Docente();
        docente.setId(id);
        return docente;
    }

    private SesionClase sesion(Long id, Horario horario, LocalDate fecha) {
        SesionClase sesion = new SesionClase();
        sesion.setId(id);
        sesion.setHorario(horario);
        sesion.setFecha(fecha);
        sesion.setEstado(SesionClaseEstado.PROGRAMADA);
        return sesion;
    }

    private void autenticarComo(String username, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(rol == Rol.ADMIN ? 1L : 2L);
        usuario.setUsername(username);
        usuario.setRol(rol);
        usuario.setActivo(true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new User(username, "x",
                                List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))),
                        "x", List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))));
        when(usuarioRepository.findByUsername(username)).thenReturn(Optional.of(usuario));
        if (rol == Rol.DOCENTE) {
            when(docenteRepository.findByUsuarioId(usuario.getId()))
                    .thenReturn(Optional.of(docente(20L)));
        }
    }

    // ===================== Prompt 6.2 =====================

    @Test
    void horarioInexistenteLanzaScheduleNotFound() {
        when(horarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sesionClaseService.obtenerOCrearSesion(99L, LocalDate.of(2026, 8, 19)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Horario no encontrado");
    }

    @Test
    void sesionExistenteSeDevuelveSinCrear() {
        Horario horario = horario(1L);
        SesionClase existente = sesion(10L, horario, LocalDate.of(2026, 8, 19));
        when(horarioRepository.findById(1L)).thenReturn(Optional.of(horario));
        when(sesionClaseRepository.findByHorarioIdAndFecha(1L, LocalDate.of(2026, 8, 19)))
                .thenReturn(Optional.of(existente));

        SesionClase resultado = sesionClaseService.obtenerOCrearSesion(1L, LocalDate.of(2026, 8, 19));

        assertThat(resultado.getId()).isEqualTo(10L);
        verify(sesionClaseCreatorTx, never()).crearSesion(any(), any());
    }

    @Test
    void sesionInexistenteSeCrea() {
        Horario horario = horario(1L);
        SesionClase nueva = sesion(20L, horario, LocalDate.of(2026, 8, 19));
        when(horarioRepository.findById(1L)).thenReturn(Optional.of(horario));
        when(sesionClaseRepository.findByHorarioIdAndFecha(1L, LocalDate.of(2026, 8, 19)))
                .thenReturn(Optional.empty());
        when(sesionClaseCreatorTx.crearSesion(horario, LocalDate.of(2026, 8, 19)))
                .thenReturn(nueva);

        SesionClase resultado = sesionClaseService.obtenerOCrearSesion(1L, LocalDate.of(2026, 8, 19));

        assertThat(resultado.getId()).isEqualTo(20L);
        verify(sesionClaseCreatorTx).crearSesion(horario, LocalDate.of(2026, 8, 19));
    }

    @Test
    void sesionCreadaNaceProgramada() {
        Horario horario = horario(1L);
        SesionClase nueva = sesion(20L, horario, LocalDate.of(2026, 8, 19));
        nueva.setEstado(SesionClaseEstado.PROGRAMADA);
        when(horarioRepository.findById(1L)).thenReturn(Optional.of(horario));
        when(sesionClaseRepository.findByHorarioIdAndFecha(1L, LocalDate.of(2026, 8, 19)))
                .thenReturn(Optional.empty());
        when(sesionClaseCreatorTx.crearSesion(horario, LocalDate.of(2026, 8, 19)))
                .thenReturn(nueva);

        SesionClase resultado = sesionClaseService.obtenerOCrearSesion(1L, LocalDate.of(2026, 8, 19));

        assertThat(resultado.getEstado()).isEqualTo(SesionClaseEstado.PROGRAMADA);
    }

    @Test
    void horaAperturaNulaEnSesionNueva() {
        Horario horario = horario(1L);
        SesionClase nueva = sesion(20L, horario, LocalDate.of(2026, 8, 19));
        when(horarioRepository.findById(1L)).thenReturn(Optional.of(horario));
        when(sesionClaseRepository.findByHorarioIdAndFecha(1L, LocalDate.of(2026, 8, 19)))
                .thenReturn(Optional.empty());
        when(sesionClaseCreatorTx.crearSesion(horario, LocalDate.of(2026, 8, 19)))
                .thenReturn(nueva);

        SesionClase resultado = sesionClaseService.obtenerOCrearSesion(1L, LocalDate.of(2026, 8, 19));

        assertThat(resultado.getHoraApertura()).isNull();
    }

    @Test
    void horaCierreNulaEnSesionNueva() {
        Horario horario = horario(1L);
        SesionClase nueva = sesion(20L, horario, LocalDate.of(2026, 8, 19));
        when(horarioRepository.findById(1L)).thenReturn(Optional.of(horario));
        when(sesionClaseRepository.findByHorarioIdAndFecha(1L, LocalDate.of(2026, 8, 19)))
                .thenReturn(Optional.empty());
        when(sesionClaseCreatorTx.crearSesion(horario, LocalDate.of(2026, 8, 19)))
                .thenReturn(nueva);

        SesionClase resultado = sesionClaseService.obtenerOCrearSesion(1L, LocalDate.of(2026, 8, 19));

        assertThat(resultado.getHoraCierre()).isNull();
    }

    @Test
    void llamadaRepetidaDevuelveLaMismaSesion() {
        Horario horario = horario(1L);
        SesionClase existente = sesion(10L, horario, LocalDate.of(2026, 8, 19));
        when(horarioRepository.findById(1L)).thenReturn(Optional.of(horario));
        when(sesionClaseRepository.findByHorarioIdAndFecha(1L, LocalDate.of(2026, 8, 19)))
                .thenReturn(Optional.of(existente));

        SesionClase s1 = sesionClaseService.obtenerOCrearSesion(1L, LocalDate.of(2026, 8, 19));
        SesionClase s2 = sesionClaseService.obtenerOCrearSesion(1L, LocalDate.of(2026, 8, 19));

        assertThat(s1.getId()).isEqualTo(s2.getId()).isEqualTo(10L);
    }

    @Test
    void mismoHorarioOtraFechaCreaSesionDistinta() {
        Horario horario = horario(1L);
        SesionClase delDia19 = sesion(10L, horario, LocalDate.of(2026, 8, 19));
        SesionClase delDia20 = sesion(11L, horario, LocalDate.of(2026, 8, 20));
        when(horarioRepository.findById(1L)).thenReturn(Optional.of(horario));
        when(sesionClaseRepository.findByHorarioIdAndFecha(1L, LocalDate.of(2026, 8, 19)))
                .thenReturn(Optional.of(delDia19));
        when(sesionClaseRepository.findByHorarioIdAndFecha(1L, LocalDate.of(2026, 8, 20)))
                .thenReturn(Optional.empty());
        when(sesionClaseCreatorTx.crearSesion(horario, LocalDate.of(2026, 8, 20)))
                .thenReturn(delDia20);

        SesionClase r1 = sesionClaseService.obtenerOCrearSesion(1L, LocalDate.of(2026, 8, 19));
        SesionClase r2 = sesionClaseService.obtenerOCrearSesion(1L, LocalDate.of(2026, 8, 20));

        assertThat(r1.getId()).isEqualTo(10L);
        assertThat(r2.getId()).isEqualTo(11L);
        assertThat(r1.getId()).isNotEqualTo(r2.getId());
    }

    @Test
    void otroHorarioMismaFechaCreaSesionDistinta() {
        Horario horarioA = horario(1L);
        Horario horarioB = horario(2L);
        SesionClase delA = sesion(10L, horarioA, LocalDate.of(2026, 8, 19));
        SesionClase delB = sesion(11L, horarioB, LocalDate.of(2026, 8, 19));
        when(horarioRepository.findById(1L)).thenReturn(Optional.of(horarioA));
        when(horarioRepository.findById(2L)).thenReturn(Optional.of(horarioB));
        when(sesionClaseRepository.findByHorarioIdAndFecha(1L, LocalDate.of(2026, 8, 19)))
                .thenReturn(Optional.of(delA));
        when(sesionClaseRepository.findByHorarioIdAndFecha(2L, LocalDate.of(2026, 8, 19)))
                .thenReturn(Optional.empty());
        when(sesionClaseCreatorTx.crearSesion(horarioB, LocalDate.of(2026, 8, 19)))
                .thenReturn(delB);

        SesionClase r1 = sesionClaseService.obtenerOCrearSesion(1L, LocalDate.of(2026, 8, 19));
        SesionClase r2 = sesionClaseService.obtenerOCrearSesion(2L, LocalDate.of(2026, 8, 19));

        assertThat(r1.getId()).isEqualTo(10L);
        assertThat(r2.getId()).isEqualTo(11L);
        assertThat(r1.getId()).isNotEqualTo(r2.getId());
    }

    @Test
    void consultarSesionExistenteNoLaModifica() {
        Horario horario = horario(1L);
        SesionClase existente = sesion(10L, horario, LocalDate.of(2026, 8, 19));
        existente.setEstado(SesionClaseEstado.CERRADA);
        when(horarioRepository.findById(1L)).thenReturn(Optional.of(horario));
        when(sesionClaseRepository.findByHorarioIdAndFecha(1L, LocalDate.of(2026, 8, 19)))
                .thenReturn(Optional.of(existente));

        SesionClase resultado = sesionClaseService.obtenerOCrearSesion(1L, LocalDate.of(2026, 8, 19));

        assertThat(resultado.getEstado()).isEqualTo(SesionClaseEstado.CERRADA);
        assertThat(resultado.getHoraApertura()).isNull();
        assertThat(resultado.getHoraCierre()).isNull();
        verify(sesionClaseRepository, never()).saveAndFlush(any());
    }

    @Test
    void carreraPerdidaRecuperaSesionPersistida() {
        Horario horario = horario(1L);
        SesionClase persistida = sesion(30L, horario, LocalDate.of(2026, 8, 19));
        when(horarioRepository.findById(1L)).thenReturn(Optional.of(horario));
        when(sesionClaseRepository.findByHorarioIdAndFecha(1L, LocalDate.of(2026, 8, 19)))
                .thenReturn(Optional.empty(), Optional.of(persistida));
        when(sesionClaseCreatorTx.crearSesion(horario, LocalDate.of(2026, 8, 19)))
                .thenReturn(null);

        SesionClase resultado = sesionClaseService.obtenerOCrearSesion(1L, LocalDate.of(2026, 8, 19));

        assertThat(resultado.getId()).isEqualTo(30L);
    }

    @Test
    void violacionNoRelacionadaNoSeEnmascara() {
        Horario horario = horario(1L);
        when(horarioRepository.findById(1L)).thenReturn(Optional.of(horario));
        when(sesionClaseRepository.findByHorarioIdAndFecha(1L, LocalDate.of(2026, 8, 19)))
                .thenReturn(Optional.empty());
        doThrow(new DataIntegrityViolationException("otra constraint rota"))
                .when(sesionClaseCreatorTx).crearSesion(horario, LocalDate.of(2026, 8, 19));

        assertThatThrownBy(() -> sesionClaseService.obtenerOCrearSesion(1L, LocalDate.of(2026, 8, 19)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("otra constraint rota");
    }

    // ===================== Prompt 6.3 — apertura =====================

    @Test
    void abrirProgramadaPasaAEstadoAbierta() {
        Docente docente = docente(20L);
        Horario horario = horario(1L, docente);
        SesionClase sesion = sesion(100L, horario, LocalDate.of(2026, 8, 19));
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));
        when(sesionClaseRepository.saveAndFlush(sesion)).thenReturn(sesion);

        var respuesta = sesionClaseService.abrirSesion(100L);

        assertThat(respuesta.estado()).isEqualTo(SesionClaseEstado.ABIERTA);
        assertThat(sesion.getEstado()).isEqualTo(SesionClaseEstado.ABIERTA);
        verify(sesionClaseRepository).saveAndFlush(sesion);
    }

    @Test
    void horaAperturaIgualAlRelojDelServidor() {
        Horario horario = horario(1L, docente(20L));
        SesionClase sesion = sesion(100L, horario, LocalDate.of(2026, 8, 19));
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));
        when(sesionClaseRepository.saveAndFlush(sesion)).thenReturn(sesion);

        var respuesta = sesionClaseService.abrirSesion(100L);

        assertThat(respuesta.horaApertura()).isEqualTo(HORA_FIJA);
        assertThat(sesion.getHoraApertura()).isEqualTo(HORA_FIJA);
    }

    @Test
    void horaCierrePermaneceNullAlAbrir() {
        Horario horario = horario(1L, docente(20L));
        SesionClase sesion = sesion(100L, horario, LocalDate.of(2026, 8, 19));
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));
        when(sesionClaseRepository.saveAndFlush(sesion)).thenReturn(sesion);

        var respuesta = sesionClaseService.abrirSesion(100L);

        assertThat(respuesta.horaCierre()).isNull();
        assertThat(sesion.getHoraCierre()).isNull();
    }

    @Test
    void sesionInexistenteLanzaSessionNotFound() {
        when(sesionClaseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sesionClaseService.abrirSesion(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sesión no encontrada");
    }

    @Test
    void adminPuedeAbrirCualquierSesion() {
        autenticarComo("admin", Rol.ADMIN);
        Docente docente = docente(20L);
        Horario horario = horario(1L, docente);
        SesionClase sesion = sesion(100L, horario, LocalDate.of(2026, 8, 19));
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));
        when(sesionClaseRepository.saveAndFlush(sesion)).thenReturn(sesion);

        var respuesta = sesionClaseService.abrirSesion(100L);

        assertThat(respuesta.estado()).isEqualTo(SesionClaseEstado.ABIERTA);
    }

    @Test
    void docentePropietarioPuedeAbrir() {
        autenticarComo("doc.a", Rol.DOCENTE);
        Docente docente = docente(20L);
        Horario horario = horario(1L, docente);
        SesionClase sesion = sesion(100L, horario, LocalDate.of(2026, 8, 19));
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));
        when(sesionClaseRepository.saveAndFlush(sesion)).thenReturn(sesion);

        var respuesta = sesionClaseService.abrirSesion(100L);

        assertThat(respuesta.estado()).isEqualTo(SesionClaseEstado.ABIERTA);
    }

    @Test
    void docenteAjenoLanzaForbiddenYNoTocaLaSesion() {
        autenticarComo("doc.a", Rol.DOCENTE);
        Docente docenteB = docente(30L);
        Horario horario = horario(1L, docenteB);
        SesionClase sesion = sesion(100L, horario, LocalDate.of(2026, 8, 19));
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));

        assertThatThrownBy(() -> sesionClaseService.abrirSesion(100L))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("otro docente");

        assertThat(sesion.getEstado()).isEqualTo(SesionClaseEstado.PROGRAMADA);
        assertThat(sesion.getHoraApertura()).isNull();
        verify(sesionClaseRepository, never()).saveAndFlush(any());
    }

    @Test
    void cerradaNoPuedeAbrirse() {
        Horario horario = horario(1L, docente(20L));
        SesionClase sesion = sesion(100L, horario, LocalDate.of(2026, 8, 19));
        sesion.setEstado(SesionClaseEstado.CERRADA);
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));

        assertThatThrownBy(() -> sesionClaseService.abrirSesion(100L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CERRADA");
        verify(sesionClaseRepository, never()).saveAndFlush(any());
    }

    @Test
    void canceladaNoPuedeAbrirse() {
        Horario horario = horario(1L, docente(20L));
        SesionClase sesion = sesion(100L, horario, LocalDate.of(2026, 8, 19));
        sesion.setEstado(SesionClaseEstado.CANCELADA);
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));

        assertThatThrownBy(() -> sesionClaseService.abrirSesion(100L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CANCELADA");
        verify(sesionClaseRepository, never()).saveAndFlush(any());
    }

    @Test
    void abiertaEsIdempotenteYConservaSuHoraApertura() {
        Horario horario = horario(1L, docente(20L));
        SesionClase sesion = sesion(100L, horario, LocalDate.of(2026, 8, 19));
        sesion.setEstado(SesionClaseEstado.ABIERTA);
        OffsetDateTime horaOriginal = OffsetDateTime.parse("2026-08-19T08:05:00-05:00");
        sesion.setHoraApertura(horaOriginal);
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));

        var respuesta = sesionClaseService.abrirSesion(100L);

        assertThat(respuesta.estado()).isEqualTo(SesionClaseEstado.ABIERTA);
        assertThat(respuesta.horaApertura()).isEqualTo(horaOriginal);
        assertThat(sesion.getHoraApertura()).isEqualTo(horaOriginal);
        verify(sesionClaseRepository, never()).saveAndFlush(any());
    }

    @Test
    void abrirNoModificaLaFechaNiElHorario() {
        Horario horario = horario(1L, docente(20L));
        SesionClase sesion = sesion(100L, horario, LocalDate.of(2026, 8, 19));
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));
        when(sesionClaseRepository.saveAndFlush(sesion)).thenReturn(sesion);

        var respuesta = sesionClaseService.abrirSesion(100L);

        assertThat(respuesta.fecha()).isEqualTo(LocalDate.of(2026, 8, 19));
        assertThat(respuesta.horarioId()).isEqualTo(1L);
        assertThat(sesion.getHorario().getId()).isEqualTo(1L);
    }

    @Test
    void abrirNoUsaHoraProvenienteDeNingunRequest() {
        Horario horario = horario(1L, docente(20L));
        SesionClase sesion = sesion(100L, horario, LocalDate.of(2026, 8, 19));
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));
        when(sesionClaseRepository.saveAndFlush(sesion)).thenReturn(sesion);

        var respuesta = sesionClaseService.abrirSesion(100L);

        assertThat(respuesta.horaApertura()).isEqualTo(HORA_FIJA);
        assertThat(sesion.getHoraApertura()).isEqualTo(HORA_FIJA);
    }

    @Test
    void docenteSinPerfilLanzaErrorControlado() {
        Usuario usuario = new Usuario();
        usuario.setId(2L);
        usuario.setUsername("doc.a");
        usuario.setRol(Rol.DOCENTE);
        usuario.setActivo(true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new User("doc.a", "x",
                                List.of(new SimpleGrantedAuthority("ROLE_DOCENTE"))),
                        "x", List.of(new SimpleGrantedAuthority("ROLE_DOCENTE"))));
        when(usuarioRepository.findByUsername("doc.a")).thenReturn(Optional.of(usuario));
        when(docenteRepository.findByUsuarioId(2L)).thenReturn(Optional.empty());

        Docente docenteB = docente(30L);
        Horario horario = horario(1L, docenteB);
        SesionClase sesion = sesion(100L, horario, LocalDate.of(2026, 8, 19));
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));

        assertThatThrownBy(() -> sesionClaseService.abrirSesion(100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("perfil docente");
        verify(sesionClaseRepository, never()).saveAndFlush(any());
    }

    @Test
    void adminAbreSesionDeOtroDocenteSinPerfilPropio() {
        autenticarComo("admin", Rol.ADMIN);
        Docente docenteB = docente(30L);
        Horario horario = horario(1L, docenteB);
        SesionClase sesion = sesion(100L, horario, LocalDate.of(2026, 8, 19));
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));
        when(sesionClaseRepository.saveAndFlush(sesion)).thenReturn(sesion);

        var respuesta = sesionClaseService.abrirSesion(100L);

        assertThat(respuesta.estado()).isEqualTo(SesionClaseEstado.ABIERTA);
        verify(docenteRepository, never()).findByUsuarioId(any());
    }

    // ===================== Prompt 6.4 — listados =====================

    @Test
    void adminListaSesionesSinFiltros() {
        when(sesionClaseRepository.buscarConFiltros(null, null, null, null, null))
                .thenReturn(List.of());

        var resultado = sesionClaseService.listarSesiones(null, null, null, null, null);

        assertThat(resultado).isEmpty();
        verify(sesionClaseRepository).buscarConFiltros(null, null, null, null, null);
    }

    @Test
    void adminListaSesionesConTodosLosFiltrosEnAnd() {
        LocalDate fecha = LocalDate.of(2026, 8, 19);
        when(sesionClaseRepository.buscarConFiltros(fecha, 30L, 7L, 3L, SesionClaseEstado.CERRADA))
                .thenReturn(List.of());

        sesionClaseService.listarSesiones(fecha, 30L, 7L, 3L, SesionClaseEstado.CERRADA);

        verify(sesionClaseRepository).buscarConFiltros(fecha, 30L, 7L, 3L, SesionClaseEstado.CERRADA);
    }

    @Test
    void docenteListaSoloSusSesionesIgnorandoDocenteIdDelRequest() {
        autenticarComo("doc.a", Rol.DOCENTE);
        when(sesionClaseRepository.buscarConFiltros(null, 20L, null, null, null))
                .thenReturn(List.of());

        sesionClaseService.listarSesiones(null, 99L, null, null, null);

        verify(sesionClaseRepository).buscarConFiltros(null, 20L, null, null, null);
    }

    @Test
    void docenteListaConFiltrosSiempreForzadoASuDocente() {
        autenticarComo("doc.a", Rol.DOCENTE);
        LocalDate fecha = LocalDate.of(2026, 8, 19);
        when(sesionClaseRepository.buscarConFiltros(fecha, 20L, 7L, 3L, SesionClaseEstado.ABIERTA))
                .thenReturn(List.of());

        sesionClaseService.listarSesiones(fecha, 55L, 7L, 3L, SesionClaseEstado.ABIERTA);

        verify(sesionClaseRepository).buscarConFiltros(fecha, 20L, 7L, 3L, SesionClaseEstado.ABIERTA);
    }

    @Test
    void listarDevuelveRespuestasConResumenesDeCursoSeccionYDocente() {
        Curso curso = new Curso();
        curso.setId(3L);
        curso.setNombre("Matemática");
        Seccion seccion = new Seccion();
        seccion.setId(7L);
        seccion.setNombre("6.º Primaria A");
        Docente docente = docente(20L);
        docente.setNombres("María");
        docente.setApellidos("López");
        Horario horario = new Horario();
        horario.setId(1L);
        horario.setCurso(curso);
        horario.setSeccion(seccion);
        horario.setDocente(docente);
        SesionClase s1 = sesion(100L, horario, LocalDate.of(2026, 8, 19));
        SesionClase s2 = sesion(101L, horario, LocalDate.of(2026, 8, 20));
        s2.setEstado(SesionClaseEstado.ABIERTA);
        when(sesionClaseRepository.buscarConFiltros(null, null, null, null, null))
                .thenReturn(List.of(s1, s2));

        var resultado = sesionClaseService.listarSesiones(null, null, null, null, null);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).id()).isEqualTo(100L);
        assertThat(resultado.get(0).horarioId()).isEqualTo(1L);
        assertThat(resultado.get(0).fecha()).isEqualTo(LocalDate.of(2026, 8, 19));
        assertThat(resultado.get(0).estado()).isEqualTo(SesionClaseEstado.PROGRAMADA);
        assertThat(resultado.get(0).curso().id()).isEqualTo(3L);
        assertThat(resultado.get(0).curso().nombre()).isEqualTo("Matemática");
        assertThat(resultado.get(0).seccion().nombre()).isEqualTo("6.º Primaria A");
        assertThat(resultado.get(0).docente().id()).isEqualTo(20L);
        assertThat(resultado.get(0).docente().nombres()).isEqualTo("María");
        assertThat(resultado.get(0).docente().apellidos()).isEqualTo("López");
        assertThat(resultado.get(1).estado()).isEqualTo(SesionClaseEstado.ABIERTA);
    }

    @Test
    void listarActivasFiltraPorEstadoAbiertaSinFiltros() {
        when(sesionClaseRepository.buscarConFiltros(null, null, null, null, SesionClaseEstado.ABIERTA))
                .thenReturn(List.of());

        var resultado = sesionClaseService.listarSesionesActivas();

        assertThat(resultado).isEmpty();
        verify(sesionClaseRepository)
                .buscarConFiltros(null, null, null, null, SesionClaseEstado.ABIERTA);
    }

    @Test
    void listarActivasNuncaIncluyeOtrosEstados() {
        Horario horario = horario(1L, docente(20L));
        SesionClase abierta = sesion(100L, horario, LocalDate.of(2026, 8, 19));
        abierta.setEstado(SesionClaseEstado.ABIERTA);
        SesionClase programada = sesion(101L, horario, LocalDate.of(2026, 8, 20));
        SesionClase cerrada = sesion(102L, horario, LocalDate.of(2026, 8, 21));
        cerrada.setEstado(SesionClaseEstado.CERRADA);
        when(sesionClaseRepository.buscarConFiltros(null, null, null, null, SesionClaseEstado.ABIERTA))
                .thenReturn(List.of(abierta, programada, cerrada));

        var resultado = sesionClaseService.listarSesionesActivas();

        assertThat(resultado).hasSize(3);
        verify(sesionClaseRepository)
                .buscarConFiltros(null, null, null, null, SesionClaseEstado.ABIERTA);
    }

    @Test
    void docenteListaActivasSoloDeSusHorarios() {
        autenticarComo("doc.a", Rol.DOCENTE);
        when(sesionClaseRepository.buscarConFiltros(null, 20L, null, null, SesionClaseEstado.ABIERTA))
                .thenReturn(List.of());

        sesionClaseService.listarSesionesActivas();

        verify(sesionClaseRepository)
                .buscarConFiltros(null, 20L, null, null, SesionClaseEstado.ABIERTA);
    }

    @Test
    void adminBuscaSesionPorIdCualquiera() {
        Horario horario = horario(1L, docente(30L));
        SesionClase sesion = sesion(100L, horario, LocalDate.of(2026, 8, 19));
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));

        var respuesta = sesionClaseService.buscarSesionPorId(100L);

        assertThat(respuesta.id()).isEqualTo(100L);
        assertThat(respuesta.horarioId()).isEqualTo(1L);
        verify(docenteRepository, never()).findByUsuarioId(any());
    }

    @Test
    void docenteBuscaSesionPropiaPorId() {
        autenticarComo("doc.a", Rol.DOCENTE);
        Horario horario = horario(1L, docente(20L));
        SesionClase sesion = sesion(100L, horario, LocalDate.of(2026, 8, 19));
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));

        var respuesta = sesionClaseService.buscarSesionPorId(100L);

        assertThat(respuesta.id()).isEqualTo(100L);
        assertThat(respuesta.estado()).isEqualTo(SesionClaseEstado.PROGRAMADA);
    }

    @Test
    void docenteBuscaSesionAjenaLanzaForbidden() {
        autenticarComo("doc.a", Rol.DOCENTE);
        Horario horario = horario(1L, docente(30L));
        SesionClase sesion = sesion(100L, horario, LocalDate.of(2026, 8, 19));
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));

        assertThatThrownBy(() -> sesionClaseService.buscarSesionPorId(100L))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("otro docente");
    }

    @Test
    void buscarSesionPorIdInexistenteLanzaSessionNotFound() {
        when(sesionClaseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sesionClaseService.buscarSesionPorId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sesión no encontrada");
    }

    @Test
    void consultasDeListadoNoGuardanNada() {
        Horario horario = horario(1L, docente(20L));
        SesionClase sesion = sesion(100L, horario, LocalDate.of(2026, 8, 19));
        when(sesionClaseRepository.buscarConFiltros(null, null, null, null, null))
                .thenReturn(List.of(sesion));
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));

        sesionClaseService.listarSesiones(null, null, null, null, null);
        sesionClaseService.listarSesionesActivas();
        sesionClaseService.buscarSesionPorId(100L);

        verify(sesionClaseRepository, never()).saveAndFlush(any());
        verify(sesionClaseRepository, never()).save(any());
    }

    // =========================================================================
    // cerrarSesion (Prompt 7.7)
    // =========================================================================

    @Test
    void cerrarSesion_idempotente_devuelveCerrada() {
        autenticarComo("admin", Rol.ADMIN);
        Horario hor = horario(10L);
        SesionClase sesion = sesion(100L, hor, LocalDate.now());
        sesion.setEstado(SesionClaseEstado.CERRADA);
        sesion.setHoraCierre(OffsetDateTime.parse("2026-08-19T14:00:00-05:00"));

        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));

        SesionClaseResponse resp = sesionClaseService.cerrarSesion(100L);
        assertThat(resp.estado()).isEqualTo(SesionClaseEstado.CERRADA);
        assertThat(resp.horaCierre()).isEqualTo(OffsetDateTime.parse("2026-08-19T14:00:00-05:00"));
        verify(sesionClaseRepository, never()).saveAndFlush(any());
        verify(asistenciaRepository, never()).saveAll(any());
    }

    @Test
    void cerrarSesion_cancelada_lanzaConflicto() {
        autenticarComo("admin", Rol.ADMIN);
        Horario hor = horario(10L);
        SesionClase sesion = sesion(100L, hor, LocalDate.now());
        sesion.setEstado(SesionClaseEstado.CANCELADA);

        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));

        assertThatThrownBy(() -> sesionClaseService.cerrarSesion(100L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CANCELADA");
    }

    @Test
    void cerrarSesion_abierta_cierraYGeneraAusentes() {
        autenticarComo("admin", Rol.ADMIN);
        Docente doc = docente(20L);
        Horario hor = horario(10L, doc);
        Seccion sec = new Seccion();
        sec.setId(99L);
        hor.setSeccion(sec);
        
        SesionClase sesion = sesion(100L, hor, LocalDate.now());
        sesion.setEstado(SesionClaseEstado.ABIERTA);

        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));

        // Estudiantes activos
        Estudiante e1 = new Estudiante(); e1.setId(1L);
        Estudiante e2 = new Estudiante(); e2.setId(2L);
        when(estudianteRepository.findBySeccionIdAndActivoTrue(99L)).thenReturn(List.of(e1, e2));

        // Solo e1 tiene asistencia
        com.aulaia.entity.Asistencia a1 = new com.aulaia.entity.Asistencia();
        a1.setEstudiante(e1);
        when(asistenciaRepository.findBySesionClaseId(100L)).thenReturn(List.of(a1));

        when(sesionClaseRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        SesionClaseResponse resp = sesionClaseService.cerrarSesion(100L);
        
        assertThat(resp.estado()).isEqualTo(SesionClaseEstado.CERRADA);
        assertThat(resp.horaCierre()).isEqualTo(HORA_FIJA);
        
        verify(asistenciaRepository).saveAll(argThat(list -> {
            List<com.aulaia.entity.Asistencia> ausentes = (List<com.aulaia.entity.Asistencia>) list;
            return ausentes.size() == 1 
                && ausentes.get(0).getEstudiante().getId().equals(2L)
                && ausentes.get(0).getEstado() == com.aulaia.entity.EstadoAsistencia.AUSENTE
                && ausentes.get(0).getMetodo() == com.aulaia.entity.MetodoRegistro.SISTEMA;
        }));
    }
}