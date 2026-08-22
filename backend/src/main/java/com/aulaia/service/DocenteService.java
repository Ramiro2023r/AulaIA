package com.aulaia.service;

import com.aulaia.dto.docente.DocenteRequest;
import com.aulaia.dto.docente.DocenteResponse;
import com.aulaia.dto.docente.DocenteUpdateRequest;
import com.aulaia.entity.Docente;
import com.aulaia.entity.Rol;
import com.aulaia.entity.Usuario;
import com.aulaia.exception.ConflictException;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.mapper.DocenteMapper;
import com.aulaia.dto.docente.DocenteProfileResponse;
import com.aulaia.dto.docente.DocenteProfileUpdateRequest;
import com.aulaia.repository.DocenteRepository;
import com.aulaia.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de negocio de docentes (Prompt 5.1, 07-PLAN).
 *
 * <p>Creación atómica (06-FLUJOS #9): se crea la cuenta {@link Usuario} con
 * rol DOCENTE y luego el perfil {@link Docente}; si el perfil falla, la
 * transacción revierte y no queda un usuario huérfano. La contraseña se
 * almacena SIEMPRE como hash BCrypt ({@link PasswordEncoder} existente del
 * proyecto); nunca en texto plano, nunca en logs, nunca en respuestas.
 *
 * <p>Flujo de creación documentado (06-FLUJOS #9): Admin → Nuevo docente →
 * Ingresar datos → Crear usuario → Asignar rol DOCENTE → Crear perfil
 * docente → Guardar. El plan menciona "crear o asociar" (07-PLAN 5.1), pero
 * ningún documento define el flujo de asociación de un usuario existente;
 * se implementa únicamente el comportamiento soportado (crear), reportando
 * la asociación como DECISIÓN NO DEFINIDA EN DOCUMENTOS.
 *
 * <p>Desactivación (06-FLUJOS #49): desactivar docente pone
 * {@code docente.activo = false} y {@code usuario.activo = false}
 * (sincronización explícitamente documentada); se mantienen históricos, sin
 * borrado físico. Sin DELETE: no está documentado.
 */
@Service
public class DocenteService {

    private static final Logger log = LoggerFactory.getLogger(DocenteService.class);

    private static final String CODE_NOT_FOUND = "TEACHER_NOT_FOUND";
    private static final String CODE_USERNAME_ALREADY_EXISTS = "USERNAME_ALREADY_EXISTS";
    private static final String CODE_USER_ALREADY_HAS_TEACHER = "USER_ALREADY_HAS_TEACHER";

    /** Nombres reales de las constraints UNIQUE (PostgreSQL, UNIQUE inline). */
    private static final String UNIQUE_USERNAME = "usuarios_username_key";
    private static final String UNIQUE_USUARIO_DOCENTE = "docentes_usuario_id_key";

    private final DocenteRepository docenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocenteMapper docenteMapper;
    private final PasswordEncoder passwordEncoder;

    public DocenteService(DocenteRepository docenteRepository,
                          UsuarioRepository usuarioRepository,
                          DocenteMapper docenteMapper,
                          PasswordEncoder passwordEncoder) {
        this.docenteRepository = docenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.docenteMapper = docenteMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<DocenteResponse> listar() {
        return docenteRepository.findAllByOrderByIdAsc().stream()
                .map(docenteMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocenteResponse buscarPorId(Long id) {
        return docenteMapper.toResponse(findOrThrow(id));
    }

    /**
     * Crea la cuenta DOCENTE y el perfil docente en una sola transacción
     * (06-FLUJOS #9). El password se hashea con BCrypt y nunca se loguea ni
     * se expone. El rol queda DOCENTE (04-BD §6.1) y ambos registros se
     * crean activos (defaults oficiales: 04-BD §6.1/§6.2).
     */
    @Transactional
    public DocenteResponse crear(DocenteRequest request) {
        String username = request.username().trim();
        validarUsernameDisponible(username);

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(Rol.DOCENTE);
        usuario.setActivo(true);
        usuario = guardarUsuario(usuario);

        Docente docente = docenteMapper.toEntity(request);
        docente.setNombres(request.nombres().trim());
        docente.setApellidos(request.apellidos().trim());
        docente.setUsuario(usuario);
        docente.setActivo(true);

        Docente guardado = guardarDocente(docente);
        log.info("Docente creado: id={}, username={}", guardado.getId(), username);
        return docenteMapper.toResponse(guardado);
    }

    /**
     * Actualiza únicamente los datos académicos autorizados
     * (nombres/apellidos). No cambia usuario, rol ni contraseña: los
     * documentos no definen cambio de credenciales desde este módulo.
     */
    @Transactional
    public DocenteResponse actualizar(Long id, DocenteUpdateRequest request) {
        Docente docente = findOrThrow(id);
        docente.setNombres(request.nombres().trim());
        docente.setApellidos(request.apellidos().trim());

        Docente guardado = guardarDocente(docente);
        log.info("Docente actualizado: id={}", guardado.getId());
        return docenteMapper.toResponse(guardado);
    }

    /**
     * Desactivación documentada (06-FLUJOS #49): {@code docente.activo =
     * false} y {@code usuario.activo = false} (un usuario inactivo no puede
     * iniciar sesión, 04-BD §6.1). Sin borrado físico: se mantienen
     * históricos. Idempotente: desactivar un docente ya inactivo conserva
     * el estado sin error. La contraseña y el hash no se tocan.
     */
    @Transactional
    public DocenteResponse desactivar(Long id) {
        Docente docente = findOrThrow(id);
        if (docente.isActivo()) {
            docente.setActivo(false);
            docente.getUsuario().setActivo(false);
            guardarUsuario(docente.getUsuario());
            docente = docenteRepository.save(docente);
            log.info("Docente desactivado: id={}", id);
        }
        return docenteMapper.toResponse(docente);
    }

    @Transactional
    public void restablecerPassword(Long id, String nuevaPassword) {
        Docente docente = findOrThrow(id);
        Usuario usuario = docente.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        guardarUsuario(usuario);
        log.info("Contraseña restablecida para el docente: id={}, username={}", id, usuario.getUsername());
    }

    @Transactional(readOnly = true)
    public DocenteProfileResponse obtenerPerfilDocente(String username) {
        Docente docente = docenteRepository.findByUsuarioUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado para el usuario: " + username, CODE_NOT_FOUND));
        return docenteMapper.toProfileResponse(docente);
    }

    @Transactional
    public DocenteProfileResponse actualizarPerfilDocente(String username, DocenteProfileUpdateRequest request) {
        Docente docente = docenteRepository.findByUsuarioUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado para el usuario: " + username, CODE_NOT_FOUND));

        docente.setCorreoAlternativo(request.correoAlternativo());
        docente.setTelefono(request.telefono());
        docente.setBiografia(request.biografia());

        Docente guardado = guardarDocente(docente);
        log.info("Perfil actualizado para el docente: username={}", username);
        return docenteMapper.toProfileResponse(guardado);
    }

    private Docente findOrThrow(Long id) {
        return docenteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado: " + id, CODE_NOT_FOUND));
    }

    private void validarUsernameDisponible(String username) {
        if (usuarioRepository.existsByUsername(username)) {
            throw new ConflictException("Ya existe un usuario con el username '" + username + "'", CODE_USERNAME_ALREADY_EXISTS);
        }
    }

    /**
     * Barrera final: la BD sigue mandando. Una violación de UNIQUE de
     * username (carrera concurrente) se convierte contextualmente en el
     * conflicto funcional; la UNIQUE de usuario_id del docente (1:1) solo
     * podría violarse en una carrera del mismo usuario, y se reporta como
     * conflicto 1:1. Cualquier otra violación se deja pasar sin enmascararla.
     * Nunca se revelan constraint ni SQL al cliente.
     */
    private Usuario guardarUsuario(Usuario usuario) {
        try {
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException ex) {
            if (esViolacion(ex, UNIQUE_USERNAME)) {
                throw new ConflictException("Ya existe un usuario con ese username", CODE_USERNAME_ALREADY_EXISTS);
            }
            throw ex;
        }
    }

    private Docente guardarDocente(Docente docente) {
        try {
            return docenteRepository.saveAndFlush(docente);
        } catch (DataIntegrityViolationException ex) {
            if (esViolacion(ex, UNIQUE_USUARIO_DOCENTE)) {
                throw new ConflictException("El usuario ya tiene un perfil docente", CODE_USER_ALREADY_HAS_TEACHER);
            }
            throw ex;
        }
    }

    private boolean esViolacion(DataIntegrityViolationException ex, String constraint) {
        Throwable causa = ex.getMostSpecificCause();
        return causa != null && causa.getMessage() != null && causa.getMessage().contains(constraint);
    }
}