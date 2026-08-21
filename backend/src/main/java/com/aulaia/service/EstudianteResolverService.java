package com.aulaia.service;

import com.aulaia.entity.Estudiante;
import com.aulaia.entity.MetodoRegistro;
import com.aulaia.exception.BusinessException;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.repository.EstudianteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Componente de resolución de estudiante para registro de asistencia
 * (Prompt 7.2 — 07-PLAN Sprint 7).
 *
 * <p>Encapsula los dos caminos de resolución documentados:
 * <ul>
 *   <li>{@link MetodoRegistro#QR} — resuelve por {@code qrToken} extraído
 *       del contenido {@code AULAIA:STUDENT:<TOKEN>}.</li>
 *   <li>{@link MetodoRegistro#CODIGO} — resuelve por código escolar exacto.</li>
 * </ul>
 *
 * <p>Ambos caminos convergen en el mismo tipo de retorno: la entidad
 * {@link Estudiante} validada para registro (07-PLAN 7.2: "Ambos caminos
 * deben converger en el mismo flujo de registro").
 *
 * <p>Códigos funcionales de error definidos en 07-PLAN 7.4:
 * <ul>
 *   <li>{@code STUDENT_NOT_FOUND} — estudiante no hallado por código o QR token.</li>
 *   <li>{@code INVALID_QR} — formato del contenido QR no coincide con el prefijo
 *       esperado {@code AULAIA:STUDENT:} (07-PLAN 4.5, 06-FLUJOS #7).</li>
 * </ul>
 *
 * <p>Este servicio NO valida si el estudiante está activo ni si pertenece a
 * la sección; esas reglas de negocio residen en {@code AsistenciaService}
 * (Prompt 7.3) para mantener el SRP.
 */
@Service
public class EstudianteResolverService {

    private static final Logger log = LoggerFactory.getLogger(EstudianteResolverService.class);

    /**
     * Prefijo oficial del contenido QR (07-PLAN 4.5, 06-FLUJOS #7).
     * Idéntico a {@code EstudianteService#PREFIJO_CONTENIDO_QR}.
     */
    static final String PREFIJO_QR = "AULAIA:STUDENT:";

    /** Código funcional: QR con formato incorrecto (07-PLAN 7.4). */
    static final String CODE_INVALID_QR = "INVALID_QR";

    /** Código funcional: estudiante no encontrado (07-PLAN 7.4). */
    static final String CODE_STUDENT_NOT_FOUND = "STUDENT_NOT_FOUND";

    private final EstudianteRepository estudianteRepository;

    public EstudianteResolverService(EstudianteRepository estudianteRepository) {
        this.estudianteRepository = estudianteRepository;
    }

    /**
     * Resuelve el {@link Estudiante} correspondiente al identificador enviado
     * por el Modo Aula, según el método de registro.
     *
     * <p>Para {@code QR}: {@code identificador} debe ser el contenido completo
     * del QR, p. ej. {@code AULAIA:STUDENT:abc123...}. Se extrae el token
     * quitando el prefijo y se busca en BD por {@code qrToken}.
     *
     * <p>Para {@code CODIGO}: {@code identificador} es el código escolar del
     * estudiante (búsqueda exacta, case-sensitive, coherente con la UNIQUE de
     * 04-BD §6.5).
     *
     * @param metodo        método de registro ({@code QR} o {@code CODIGO}).
     * @param identificador contenido del QR o código escolar.
     * @return entidad {@link Estudiante} encontrada.
     * @throws BusinessException         si el contenido QR tiene formato inválido
     *                                   (código {@code INVALID_QR}).
     * @throws ResourceNotFoundException si no existe estudiante con el token o
     *                                   código dado (código {@code STUDENT_NOT_FOUND}).
     * @throws IllegalArgumentException  si {@code metodo} no es QR ni CODIGO
     *                                   (caso no documentado; falla rápido).
     */
    @Transactional(readOnly = true)
    public Estudiante resolver(MetodoRegistro metodo, String identificador) {
        return switch (metodo) {
            case QR -> resolverPorQr(identificador);
            case CODIGO -> resolverPorCodigo(identificador);
            default -> throw new IllegalArgumentException(
                    "Método no soportado en resolución de estudiante: " + metodo);
        };
    }

    // -------------------------------------------------------------------------
    // Métodos privados de resolución
    // -------------------------------------------------------------------------

    /**
     * Resuelve por contenido QR.
     *
     * <p>Valida que el contenido comience con {@code AULAIA:STUDENT:} (07-PLAN
     * 4.5). Extrae el token y lo busca por {@code qrToken} en BD.
     */
    private Estudiante resolverPorQr(String contenidoQr) {
        if (contenidoQr == null || !contenidoQr.startsWith(PREFIJO_QR)) {
            log.warn("Formato QR inválido: contenido='{}' (no comienza con {})",
                    ocultarContenido(contenidoQr), PREFIJO_QR);
            throw new BusinessException(
                    "El contenido del QR no tiene el formato esperado (AULAIA:STUDENT:<TOKEN>)",
                    CODE_INVALID_QR);
        }

        String token = contenidoQr.substring(PREFIJO_QR.length());

        if (token.isBlank()) {
            log.warn("Formato QR inválido: token vacío tras extraer prefijo");
            throw new BusinessException(
                    "El contenido del QR no contiene un token válido",
                    CODE_INVALID_QR);
        }

        return estudianteRepository.findByQrToken(token)
                .orElseThrow(() -> {
                    log.warn("Estudiante no encontrado por qrToken (contenido QR recibido, token omitido en log)");
                    return new ResourceNotFoundException(
                            "Estudiante no encontrado para el QR escaneado",
                            CODE_STUDENT_NOT_FOUND);
                });
    }

    /**
     * Resuelve por código escolar exacto (04-BD §6.5: UNIQUE sobre {@code codigo}).
     */
    private Estudiante resolverPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException(
                    "El código escolar no puede estar vacío",
                    CODE_INVALID_QR);          // reutilizamos semántica de entrada inválida
        }

        return estudianteRepository.findByCodigo(codigo.trim())
                .orElseThrow(() -> {
                    log.warn("Estudiante no encontrado: codigo='{}'", codigo);
                    return new ResourceNotFoundException(
                            "Estudiante no encontrado para el código: " + codigo,
                            CODE_STUDENT_NOT_FOUND);
                });
    }

    /**
     * Oculta el contenido real del QR para no exponer tokens en logs.
     * Muestra solo si es null o los primeros {@value #PREFIJO_QR_LOG_MAX} caracteres.
     */
    private static final int PREFIJO_QR_LOG_MAX = 20;

    private static String ocultarContenido(String contenido) {
        if (contenido == null) return "<null>";
        if (contenido.length() <= PREFIJO_QR_LOG_MAX) return contenido + " [completo]";
        return contenido.substring(0, PREFIJO_QR_LOG_MAX) + "...[ocultado]";
    }
}
