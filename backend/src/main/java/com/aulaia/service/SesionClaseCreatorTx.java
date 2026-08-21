package com.aulaia.service;

import com.aulaia.entity.Horario;
import com.aulaia.entity.SesionClase;
import com.aulaia.entity.SesionClaseEstado;
import com.aulaia.repository.SesionClaseRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Punto de inserción de una nueva {@link SesionClase} en transacción
 * independiente (Prompt 6.2).
 *
 * <p>La UNIQUE física {@code uq_sesion_horario_fecha} (04-BD §7.1) es la
 * barrera final contra duplicados bajo concurrencia. Si dos peticiones
 * simultáneas intentan crear la misma sesión, solo una transacción
 * inserta; la perdedora recibe {@link DataIntegrityViolationException} y
 * retorna {@code null} (el Service re-consulta y devuelve la fila
 * persistida). Al ejecutarse en {@code REQUIRES_NEW}, la transacción
 * abortada no contamina la transacción del llamador (PostgreSQL anula la
 * transacción tras una violación de UNIQUE) y la re-consulta del Service
 * puede ejecutarse con seguridad.
 *
 * <p>Solo se reconoce la violación de la UNIQUE documentada
 * ({@code uq_sesion_horario_fecha}); cualquier otra violación se propaga
 * sin enmascararse (sin exponer SQL ni detalle al cliente, lo maneja el
 * manejador global de errores).
 */
@Component
class SesionClaseCreatorTx {

    private static final String UNIQUE_UQ_SESION = "uq_sesion_horario_fecha";

    private final SesionClaseRepository sesionClaseRepository;

    SesionClaseCreatorTx(SesionClaseRepository sesionClaseRepository) {
        this.sesionClaseRepository = sesionClaseRepository;
    }

    /**
     * Intenta persistir la sesión (horario + fecha, estado inicial
     * PROGRAMADA — 04-BD §7.1 DEFAULT). Devuelve la sesión guardada o
     * {@code null} si otra transacción ganó la carrera de la UNIQUE.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    SesionClase crearSesion(Horario horario, LocalDate fecha) {
        SesionClase sesion = new SesionClase();
        sesion.setHorario(horario);
        sesion.setFecha(fecha);
        sesion.setEstado(SesionClaseEstado.PROGRAMADA);
        try {
            return sesionClaseRepository.saveAndFlush(sesion);
        } catch (DataIntegrityViolationException ex) {
            if (esViolacionUqSesion(ex)) {
                return null;
            }
            throw ex;
        }
    }

    private boolean esViolacionUqSesion(DataIntegrityViolationException ex) {
        Throwable causa = ex.getMostSpecificCause();
        return causa != null && causa.getMessage() != null && causa.getMessage().contains(UNIQUE_UQ_SESION);
    }
}