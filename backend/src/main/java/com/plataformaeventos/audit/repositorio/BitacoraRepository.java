package com.plataformaeventos.audit.repositorio;

import com.plataformaeventos.audit.modelo.Bitacora;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;

public interface BitacoraRepository extends Repository<Bitacora, Long> {
    Bitacora save(Bitacora bitacora);

    @Query("""
        select b from Bitacora b where b.organizacion.id = :organizacion
        and (:modulo is null or lower(b.modulo) like lower(concat('%', :modulo, '%')))
        and (:entidad is null or lower(b.entidad) like lower(concat('%', :entidad, '%')))
        and (:accion is null or lower(b.accion) like lower(concat('%', :accion, '%')))
        and (:exito is null or b.exito = :exito)
        and (:actor is null or b.actor.id = :actor)
        and (:desde is null or b.fecha >= :desde)
        and (:hasta is null or b.fecha < :hasta)
        """)
    Page<Bitacora> buscar(@Param("organizacion") Long organizacion, @Param("modulo") String modulo,
                          @Param("entidad") String entidad, @Param("accion") String accion,
                          @Param("exito") Boolean exito, @Param("actor") Long actor,
                          @Param("desde") OffsetDateTime desde, @Param("hasta") OffsetDateTime hasta,
                          Pageable pageable);
}
