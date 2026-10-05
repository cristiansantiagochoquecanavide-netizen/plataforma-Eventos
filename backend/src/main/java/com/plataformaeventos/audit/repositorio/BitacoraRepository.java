package com.plataformaeventos.audit.repositorio;

import com.plataformaeventos.audit.modelo.Bitacora;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public interface BitacoraRepository extends Repository<Bitacora, Long>, JpaSpecificationExecutor<Bitacora> {
    Bitacora save(Bitacora bitacora);

    default Page<Bitacora> buscar(Long organizacion, String modulo, String entidad, String accion,
                                  Boolean exito, Long actor, OffsetDateTime desde, OffsetDateTime hasta,
                                  Pageable pageable) {
        Specification<Bitacora> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizacion").get("id"), organizacion));
            if (modulo != null && !modulo.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("modulo")), "%" + modulo.trim().toLowerCase() + "%"));
            }
            if (entidad != null && !entidad.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("entidad")), "%" + entidad.trim().toLowerCase() + "%"));
            }
            if (accion != null && !accion.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("accion")), "%" + accion.trim().toLowerCase() + "%"));
            }
            if (exito != null) {
                predicates.add(cb.equal(root.get("exito"), exito));
            }
            if (actor != null) {
                predicates.add(cb.equal(root.get("actor").get("id"), actor));
            }
            if (desde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde));
            }
            if (hasta != null) {
                predicates.add(cb.lessThan(root.get("fecha"), hasta));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }
}
