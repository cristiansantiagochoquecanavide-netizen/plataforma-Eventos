package com.plataformaeventos.notificaciones.repositorio;

import com.plataformaeventos.notificaciones.modelo.EstadoEnvioNotificacion;
import com.plataformaeventos.notificaciones.modelo.NotificacionEvento;
import com.plataformaeventos.notificaciones.modelo.TipoNotificacionEvento;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificacionEventoRepository extends JpaRepository<NotificacionEvento, Long> {
    boolean existsByOrganizacionIdAndClaveDeduplicacion(Long organizacionId, String claveDeduplicacion);

    List<NotificacionEvento> findTop100ByOrganizacionIdAndEventoIdOrderByFechaProgramadaDesc(
            Long organizacionId, Long eventoId);

    Optional<NotificacionEvento> findByIdAndOrganizacionId(Long id, Long organizacionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NotificacionEvento n join fetch n.evento join fetch n.inscripcion join fetch n.usuario "
            + "where n.estadoEnvio = :estado and n.fechaProgramada <= :ahora order by n.fechaProgramada asc")
    List<NotificacionEvento> bloquearPendientes(@Param("estado") EstadoEnvioNotificacion estado,
                                                @Param("ahora") OffsetDateTime ahora, Pageable pageable);

    @Modifying
    @Query("update NotificacionEvento n set n.estadoEnvio = :cancelado, n.ultimoError = :motivo "
            + "where n.evento.id = :evento and n.organizacion.id = :org and n.estadoEnvio = :pendiente "
            + "and n.tipo in :tipos")
    int cancelarPendientesPorEvento(@Param("evento") Long eventoId, @Param("org") Long organizacionId,
                                    @Param("pendiente") EstadoEnvioNotificacion pendiente,
                                    @Param("cancelado") EstadoEnvioNotificacion cancelado,
                                    @Param("tipos") List<TipoNotificacionEvento> tipos,
                                    @Param("motivo") String motivo);

    @Modifying
    @Query("update NotificacionEvento n set n.estadoEnvio = :cancelado, n.ultimoError = :motivo "
            + "where n.inscripcion.id = :inscripcion and n.organizacion.id = :org and n.estadoEnvio = :pendiente")
    int cancelarPendientesPorInscripcion(@Param("inscripcion") Long idInscripcion, @Param("org") Long organizacionId,
                                         @Param("pendiente") EstadoEnvioNotificacion pendiente,
                                         @Param("cancelado") EstadoEnvioNotificacion cancelado,
                                         @Param("motivo") String motivo);
}
