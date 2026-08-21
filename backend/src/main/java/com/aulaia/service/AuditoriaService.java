package com.aulaia.service;

import com.aulaia.dto.auditoria.AuditoriaResponse;
import com.aulaia.entity.Auditoria;
import com.aulaia.repository.AuditoriaRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public List<AuditoriaResponse> buscarConFiltros(
            String usuario,
            String entidad,
            String accion,
            LocalDate desde,
            LocalDate hasta) {

        Specification<Auditoria> spec = buildSpec(usuario, entidad, accion, desde, hasta);

        return auditoriaRepository.findAll(spec,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "fechaHora"))
                .stream()
                .map(a -> new AuditoriaResponse(
                        a.getId(),
                        a.getUsuario() != null ? a.getUsuario().getUsername() : null,
                        a.getEntidad(),
                        a.getEntidadId(),
                        a.getAccion(),
                        a.getValorAnterior(),
                        a.getValorNuevo(),
                        a.getIpOrigen(),
                        a.getFechaHora()))
                .toList();
    }

    private Specification<Auditoria> buildSpec(String usuario, String entidad,
                                                String accion, LocalDate desde, LocalDate hasta) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!isBlank(usuario)) {
                var join = root.join("usuario", jakarta.persistence.criteria.JoinType.LEFT);
                predicates.add(cb.like(cb.lower(join.get("username")),
                        "%" + usuario.toLowerCase() + "%"));
            }
            if (!isBlank(entidad)) {
                predicates.add(cb.like(cb.lower(root.get("entidad")),
                        "%" + entidad.toLowerCase() + "%"));
            }
            if (!isBlank(accion)) {
                predicates.add(cb.like(cb.lower(root.get("accion")),
                        "%" + accion.toLowerCase() + "%"));
            }
            if (desde != null) {
                OffsetDateTime desdeOdt = desde.atStartOfDay().atOffset(ZoneOffset.UTC);
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaHora"), desdeOdt));
            }
            if (hasta != null) {
                OffsetDateTime hastaOdt = hasta.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
                predicates.add(cb.lessThan(root.get("fechaHora"), hastaOdt));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

