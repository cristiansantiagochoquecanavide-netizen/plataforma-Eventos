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
        and (:moduloIdx is null or (b.indiceModulo = :moduloIdx or (b.indiceModulo is null and lower(trim(b.modulo)) = :moduloNormalizado)))
        and (:entidadIdx is null or (b.indiceEntidad = :entidadIdx or (b.indiceEntidad is null and lower(trim(b.entidad)) = :entidadNormalizada)))
        and (:accionIdx is null or (b.indiceAccion = :accionIdx or (b.indiceAccion is null and lower(trim(b.accion)) = :accionNormalizada)))
        and (:exitoIdx is null or (b.indiceExito = :exitoIdx or (b.indiceExito is null and b.exito = :exito)))
        and (:actorIdx is null or (b.indiceActor = :actorIdx or (b.indiceActor is null and b.actor.id = :actor)))
        and (cast(:desde as OffsetDateTime) is null or b.fecha >= :desde)
        and (cast(:hasta as OffsetDateTime) is null or b.fecha < :hasta)
        """)
    Page<Bitacora> buscar(@Param("organizacion") Long organizacion,
                          @Param("moduloIdx") String moduloIdx, @Param("moduloNormalizado") String moduloNormalizado,
                          @Param("entidadIdx") String entidadIdx, @Param("entidadNormalizada") String entidadNormalizada,
                          @Param("accionIdx") String accionIdx, @Param("accionNormalizada") String accionNormalizada,
                          @Param("exitoIdx") String exitoIdx, @Param("exito") Boolean exito,
                          @Param("actorIdx") String actorIdx, @Param("actor") Long actor,
                          @Param("desde") OffsetDateTime desde, @Param("hasta") OffsetDateTime hasta,
                          Pageable pageable);
}
