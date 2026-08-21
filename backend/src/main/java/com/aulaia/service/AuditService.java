package com.aulaia.service;

import com.aulaia.entity.Auditoria;
import com.aulaia.entity.Usuario;
import com.aulaia.repository.AuditoriaRepository;
import com.aulaia.repository.UsuarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de auditoría para registrar operaciones clave (07-PLAN 14.1).
 */
@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditoriaRepository auditoriaRepository,
                        UsuarioRepository usuarioRepository,
                        ObjectMapper objectMapper) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Registra una acción de auditoría de manera síncrona/transaccional.
     *
     * @param entidad      Nombre de la entidad (e.g. "asistencias", "horarios")
     * @param entidadId    ID de la entidad afectada (nullable)
     * @param accion       Tipo de acción (e.g. "MODIFICAR_ASISTENCIA", "GENERAR_NUEVO_QR")
     * @param valorAnterior Objeto anterior (será serializado a JSON)
     * @param valorNuevo    Objeto nuevo (será serializado a JSON)
     */
    @Transactional
    public void registrar(String entidad, Long entidadId, String accion, Object valorAnterior, Object valorNuevo) {
        Auditoria auditoria = new Auditoria();
        auditoria.setEntidad(entidad);
        auditoria.setEntidadId(entidadId);
        auditoria.setAccion(accion);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String username = auth.getName();
            usuarioRepository.findByUsername(username).ifPresent(auditoria::setUsuario);
            // El IP de origen se omitirá en esta versión a menos que pasemos un interceptor de request
        }

        try {
            if (valorAnterior != null) {
                auditoria.setValorAnterior(objectMapper.writeValueAsString(valorAnterior));
            }
            if (valorNuevo != null) {
                auditoria.setValorNuevo(objectMapper.writeValueAsString(valorNuevo));
            }
        } catch (JsonProcessingException e) {
            logger.error("Error serializando JSON de auditoría para acción {}", accion, e);
        }

        auditoriaRepository.save(auditoria);
    }
}
